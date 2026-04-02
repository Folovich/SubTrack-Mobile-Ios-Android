package com.subscriptionmanager.importing.service;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.common.enums.SourceType;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.entity.Category;
import com.subscriptionmanager.entity.ImportItem;
import com.subscriptionmanager.entity.ImportJob;
import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.importing.ImportItemStatus;
import com.subscriptionmanager.importing.ImportJobStatus;
import com.subscriptionmanager.importing.dto.ImportErrorItemResponse;
import com.subscriptionmanager.importing.dto.ImportHistoryItemResponse;
import com.subscriptionmanager.importing.dto.ImportItemResultResponse;
import com.subscriptionmanager.importing.dto.ImportResultResponse;
import com.subscriptionmanager.importing.dto.ImportStartRequest;
import com.subscriptionmanager.importing.dto.MailMessageRequest;
import com.subscriptionmanager.importing.exception.ImportConsentRequiredException;
import com.subscriptionmanager.importing.parser.GmailNetflixParser;
import com.subscriptionmanager.importing.provider.ImportProviderAvailabilityPolicy;
import com.subscriptionmanager.integration.provider.MailImportProvider;
import com.subscriptionmanager.integration.provider.MailImportProviderRegistry;
import com.subscriptionmanager.repository.CategoryRepository;
import com.subscriptionmanager.repository.ConsentRepository;
import com.subscriptionmanager.repository.ImportItemRepository;
import com.subscriptionmanager.repository.ImportJobRepository;
import com.subscriptionmanager.repository.SubscriptionRepository;
import com.subscriptionmanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ImportServiceImpl implements ImportService {
    private static final String META_SEPARATOR = "|";
    private static final String STATUS_KEY = "STATUS=";
    private static final String REASON_KEY = "REASON=";

    private final ImportJobRepository importJobRepository;
    private final ImportItemRepository importItemRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CategoryRepository categoryRepository;
    private final ConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final GmailNetflixParser gmailNetflixParser;
    private final ImportProviderAvailabilityPolicy importProviderAvailabilityPolicy;
    private final MailImportProviderRegistry mailImportProviderRegistry;

    public ImportServiceImpl(
            ImportJobRepository importJobRepository,
            ImportItemRepository importItemRepository,
            SubscriptionRepository subscriptionRepository,
            CategoryRepository categoryRepository,
            ConsentRepository consentRepository,
            UserRepository userRepository,
            GmailNetflixParser gmailNetflixParser,
            ImportProviderAvailabilityPolicy importProviderAvailabilityPolicy,
            MailImportProviderRegistry mailImportProviderRegistry
    ) {
        this.importJobRepository = importJobRepository;
        this.importItemRepository = importItemRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.categoryRepository = categoryRepository;
        this.consentRepository = consentRepository;
        this.userRepository = userRepository;
        this.gmailNetflixParser = gmailNetflixParser;
        this.importProviderAvailabilityPolicy = importProviderAvailabilityPolicy;
        this.mailImportProviderRegistry = mailImportProviderRegistry;
    }

    @Override
    @Transactional
    public ImportResultResponse start(Long userId, ImportStartRequest request) {
        if (request == null) {
            throw new ApiException("request is required");
        }
        if (request.messages() == null || request.messages().isEmpty()) {
            throw new ApiException("messages must contain at least one message");
        }

        ImportProvider provider = importProviderAvailabilityPolicy.requireEnabledForImport(request.provider());
        importProviderAvailabilityPolicy.requireImplementedForImport(provider);
        ensureActiveConsent(userId, provider);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        return processMessages(user, provider, request.messages());
    }

    @Override
    @Transactional
    public ImportResultResponse syncMailbox(Long userId, String providerRaw) {
        ImportProvider provider = importProviderAvailabilityPolicy.resolveEnabledForConsent(providerRaw);
        importProviderAvailabilityPolicy.requireEnabledForMailboxFlow(provider);
        ensureActiveConsent(userId, provider);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        MailImportProvider providerService = mailImportProviderRegistry.require(provider);
        MailImportProvider.MailboxFetchResult fetchResult = providerService.fetchMailboxMessages(userId);
        ImportResultResponse result = processMessages(user, provider, fetchResult.messages());
        providerService.markSyncCompleted(userId, fetchResult.fetchedAt());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportHistoryItemResponse> history(Long userId) {
        return importJobRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(job -> new ImportHistoryItemResponse(
                        job.getId(),
                        job.getProvider().name(),
                        job.getStatus(),
                        job.getStartedAt(),
                        job.getFinishedAt()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ImportResultResponse getById(Long userId, Long jobId) {
        ImportJob job = importJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ApiException("Import job not found"));

        List<ImportItem> items = importItemRepository.findByJobIdOrderByIdAsc(job.getId());
        int created = (int) items.stream().filter(item -> item.getStatus() == ImportItemStatus.IMPORTED).count();
        int skipped = (int) items.stream().filter(item -> item.getStatus() == ImportItemStatus.SKIPPED).count();
        int errors = items.size() - created - skipped;
        List<ImportErrorItemResponse> errorItems = items.stream()
                .filter(item -> item.getStatus() != ImportItemStatus.IMPORTED && item.getStatus() != ImportItemStatus.SKIPPED)
                .map(item -> new ImportErrorItemResponse(
                        item.getExternalId(),
                        item.getReason() == null ? "unknown parser/import error" : item.getReason()
                ))
                .toList();

        return new ImportResultResponse(
                job.getId(),
                job.getProvider().name(),
                job.getStatus(),
                items.size(),
                created,
                skipped,
                errors,
                job.getStartedAt(),
                job.getFinishedAt(),
                errorItems,
                items.stream().map(this::toItemResponse).toList()
        );
    }

    private ImportResultResponse processMessages(User user, ImportProvider provider, List<MailMessageRequest> messages) {
        ImportJob job = new ImportJob();
        job.setUser(user);
        job.setProvider(provider);
        job.setStatus(ImportJobStatus.IN_PROGRESS.name());
        job.setStartedAt(OffsetDateTime.now());
        job.setErrorMessage(null);
        job = importJobRepository.save(job);

        int created = 0;
        int skipped = 0;
        int errors = 0;
        List<ImportErrorItemResponse> errorItems = new ArrayList<>();
        List<ImportItemResultResponse> items = new ArrayList<>();

        for (MailMessageRequest message : messages) {
            String externalId = normalizeExternalId(message.externalId());
            if (externalId == null) {
                errors++;
                errorItems.add(new ImportErrorItemResponse(message.externalId(), "externalId is blank"));
                ImportItem item = saveImportItem(job, message, "unknown", ImportItemStatus.PARSE_ERROR, "externalId is blank", null, null);
                items.add(toItemResponse(item));
                continue;
            }

            if (importItemRepository.existsByJobUserIdAndExternalId(user.getId(), externalId)) {
                skipped++;
                ImportItem item = saveImportItem(job, message, externalId, ImportItemStatus.SKIPPED, "duplicate externalId", null, null);
                items.add(toItemResponse(item));
                continue;
            }

            GmailNetflixParser.ParseResult parseResult = gmailNetflixParser.parse(message);
            if (!parseResult.success()) {
                ImportItemStatus itemStatus = parseResult.unsupported()
                        ? ImportItemStatus.UNSUPPORTED
                        : ImportItemStatus.PARSE_ERROR;
                errors++;
                errorItems.add(new ImportErrorItemResponse(externalId, parseResult.reason()));
                ImportItem item = saveImportItem(job, message, externalId, itemStatus, parseResult.reason(), parseResult.parsedSubscription(), null);
                items.add(toItemResponse(item));
                continue;
            }

            GmailNetflixParser.ParsedSubscription parsed = parseResult.parsedSubscription();
            String fingerprint = buildFingerprint(provider, parsed);
            boolean duplicateSubscription = subscriptionRepository.existsByUserIdAndImportFingerprintAndStatus(
                    user.getId(),
                    fingerprint,
                    SubscriptionStatus.ACTIVE
            ) || subscriptionRepository.existsByUserIdAndServiceNameIgnoreCaseAndAmountAndCurrencyAndBillingPeriodAndNextBillingDateAndStatus(
                    user.getId(),
                    parsed.serviceName(),
                    parsed.amount(),
                    parsed.currency(),
                    parsed.billingPeriod(),
                    parsed.nextBillingDate(),
                    SubscriptionStatus.ACTIVE
            );

            if (duplicateSubscription) {
                skipped++;
                ImportItem item = saveImportItem(job, message, externalId, ImportItemStatus.SKIPPED, "duplicate subscription", parsed, null);
                items.add(toItemResponse(item));
                continue;
            }

            Subscription subscription = new Subscription();
            subscription.setUser(user);
            subscription.setServiceName(parsed.serviceName());
            subscription.setAmount(parsed.amount());
            subscription.setCurrency(parsed.currency().toUpperCase(Locale.ROOT));
            subscription.setBillingPeriod(parsed.billingPeriod());
            subscription.setNextBillingDate(parsed.nextBillingDate());
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setSourceType(SourceType.MAIL_IMPORT);
            subscription.setImportFingerprint(fingerprint);

            Optional<Category> categoryOptional = categoryRepository.findByNameIgnoreCase(parsed.categoryName());
            subscription.setCategory(categoryOptional.orElse(null));
            subscription = subscriptionRepository.save(subscription);

            created++;
            ImportItem item = saveImportItem(job, message, externalId, ImportItemStatus.IMPORTED, null, parsed, subscription);
            items.add(toItemResponse(item));
        }

        int processed = messages.size();
        job.setFinishedAt(OffsetDateTime.now());
        job.setStatus(resolveFinalStatus(processed, created, errors));
        job.setErrorMessage(errorItems.isEmpty() ? null : errorItemsToString(errorItems));
        job = importJobRepository.save(job);

        return new ImportResultResponse(
                job.getId(),
                job.getProvider().name(),
                job.getStatus(),
                processed,
                created,
                skipped,
                errors,
                job.getStartedAt(),
                job.getFinishedAt(),
                errorItems,
                items
        );
    }

    private void ensureActiveConsent(Long userId, ImportProvider provider) {
        boolean hasActiveConsent = consentRepository.existsByUserIdAndProviderAndRevokedAtIsNull(userId, provider);
        if (!hasActiveConsent) {
            throw new ImportConsentRequiredException(provider);
        }
    }

    private String normalizeExternalId(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return null;
        }
        return externalId.trim();
    }

    private ImportItem saveImportItem(
            ImportJob job,
            MailMessageRequest message,
            String externalId,
            ImportItemStatus status,
            String reason,
            GmailNetflixParser.ParsedSubscription parsed,
            Subscription subscription
    ) {
        ImportItem item = new ImportItem();
        item.setJob(job);
        item.setExternalId(externalId == null ? "missing-external-id" : externalId);
        item.setStatus(status);
        item.setReason(reason);
        item.setMessageReceivedAt(message.receivedAt());
        item.setSourceProvider(parsed != null ? parsed.sourceProvider() : sourceProviderFromEmail(message.from()));
        item.setServiceName(parsed != null ? parsed.serviceName() : null);
        item.setAmount(parsed != null ? parsed.amount() : null);
        item.setCurrency(parsed != null ? parsed.currency() : null);
        item.setBillingPeriod(parsed != null ? parsed.billingPeriod() : null);
        item.setNextBillingDate(parsed != null ? parsed.nextBillingDate() : null);
        item.setCategoryName(parsed != null ? parsed.categoryName() : null);
        item.setSubscription(subscription);
        item.setRawPayload(toRawPayload(status, reason, message));
        item.setCreatedAt(OffsetDateTime.now());
        return importItemRepository.save(item);
    }

    private String sourceProviderFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "unknown";
        }
        return email.substring(email.indexOf('@') + 1).toLowerCase(Locale.ROOT);
    }

    private String toRawPayload(ImportItemStatus status, String reason, MailMessageRequest message) {
        String sanitizedReason = reason == null ? "" : reason.replace("\n", " ").replace("\r", " ");
        String sanitizedSubject = message.subject().replace("\n", " ").replace("\r", " ");
        return STATUS_KEY + status.name()
                + META_SEPARATOR + REASON_KEY + sanitizedReason
                + META_SEPARATOR + "FROM=" + message.from()
                + META_SEPARATOR + "SUBJECT=" + sanitizedSubject;
    }

    private String resolveFinalStatus(int processed, int created, int errors) {
        if (processed > 0 && created == 0 && errors == processed) {
            return ImportJobStatus.FAILED.name();
        }
        if (errors > 0) {
            return ImportJobStatus.COMPLETED_WITH_ERRORS.name();
        }
        return ImportJobStatus.COMPLETED.name();
    }

    private String errorItemsToString(List<ImportErrorItemResponse> errorItems) {
        return errorItems.stream()
                .map(error -> error.externalId() + ": " + error.reason())
                .reduce((left, right) -> left + "\n" + right)
                .orElse(null);
    }

    private ImportItemResultResponse toItemResponse(ImportItem item) {
        return new ImportItemResultResponse(
                item.getExternalId(),
                item.getStatus().name(),
                item.getReason(),
                item.getSourceProvider(),
                item.getServiceName(),
                item.getAmount(),
                item.getCurrency(),
                item.getBillingPeriod() == null ? null : item.getBillingPeriod().name(),
                item.getNextBillingDate(),
                item.getCategoryName(),
                item.getMessageReceivedAt()
        );
    }

    private String buildFingerprint(ImportProvider provider, GmailNetflixParser.ParsedSubscription parsed) {
        String rawFingerprint = provider.name()
                + "|" + parsed.serviceName().toLowerCase(Locale.ROOT)
                + "|" + scaled(parsed.amount())
                + "|" + parsed.currency().toUpperCase(Locale.ROOT)
                + "|" + parsed.billingPeriod().name()
                + "|" + parsed.nextBillingDate();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawFingerprint.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new ApiException("failed to build import fingerprint");
        }
    }

    private String scaled(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}

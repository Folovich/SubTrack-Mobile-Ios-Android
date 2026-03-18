package com.subscriptionmanager.subscription.support;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.common.enums.SourceType;
import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.entity.SupportEmailEvent;
import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.exception.SubscriptionNotFoundException;
import com.subscriptionmanager.exception.SubscriptionOwnershipException;
import com.subscriptionmanager.repository.SubscriptionRepository;
import com.subscriptionmanager.repository.SupportEmailEventRepository;
import com.subscriptionmanager.subscription.dto.SupportEmailDraftDetailsResponse;
import com.subscriptionmanager.subscription.dto.SupportEmailDraftResponse;
import com.subscriptionmanager.subscription.dto.SupportEmailEventRequest;
import com.subscriptionmanager.subscription.support.SupportEmailTemplateRegistry.SupportEmailTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SupportEmailDraftServiceImpl implements SupportEmailDraftService {

    private static final int MAX_SERVICE_NAME_LENGTH = 120;
    private static final int MAX_AMOUNT_LENGTH = 32;
    private static final int MAX_CURRENCY_LENGTH = 8;
    private static final int MAX_BILLING_PERIOD_LENGTH = 32;
    private static final int MAX_DATE_LENGTH = 32;
    private static final int MAX_ACCOUNT_EMAIL_LENGTH = 254;
    private static final int MAX_USER_NAME_LENGTH = 120;
    private static final int MAX_SUBJECT_LENGTH = 200;
    private static final int MAX_BODY_LENGTH = 5000;
    private static final int MAX_PLAIN_TEXT_LENGTH = 6500;
    private static final String DEFAULT_SUBJECT = "Subscription support request";

    private static final Pattern CONTROL_CHARS_PATTERN = Pattern.compile("[\\x00-\\x1F\\x7F]");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s{2,}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final SubscriptionRepository subscriptionRepository;
    private final SupportEmailEventRepository supportEmailEventRepository;
    private final SupportEmailTemplateRegistry templateRegistry;

    public SupportEmailDraftServiceImpl(
            SubscriptionRepository subscriptionRepository,
            SupportEmailEventRepository supportEmailEventRepository,
            SupportEmailTemplateRegistry templateRegistry
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.supportEmailEventRepository = supportEmailEventRepository;
        this.templateRegistry = templateRegistry;
    }

    @Override
    @Transactional(readOnly = true)
    public SupportEmailDraftResponse getDraft(Long userId, Long subscriptionId, SupportEmailAction action) {
        if (action == null) {
            throw new ApiException("action is required");
        }

        Subscription subscription = requireOwnedSubscription(userId, subscriptionId);
        User user = subscription.getUser();
        String provider = resolveProvider(subscription);
        SupportEmailTemplate template = templateRegistry.resolve(subscription.getServiceName(), provider);

        Map<String, String> placeholders = buildPlaceholders(subscription, user);
        String subjectTemplate = action == SupportEmailAction.CANCEL
                ? template.cancelSubjectTemplate()
                : template.pauseSubjectTemplate();
        String bodyTemplate = action == SupportEmailAction.CANCEL
                ? template.cancelBodyTemplate()
                : template.pauseBodyTemplate();

        String subject = sanitizeSubject(applyTemplate(subjectTemplate, placeholders));
        String body = sanitizeBody(applyTemplate(bodyTemplate, placeholders));
        String to = sanitizeEmail(template.supportEmail());
        String mailtoUrl = buildMailtoUrl(to, subject, body);
        String plainTextForCopy = truncate("To: " + to + "\nSubject: " + subject + "\n\n" + body, MAX_PLAIN_TEXT_LENGTH);

        return new SupportEmailDraftResponse(
                subscription.getId(),
                action,
                provider,
                new SupportEmailDraftDetailsResponse(to, subject, body, mailtoUrl, plainTextForCopy)
        );
    }

    @Override
    @Transactional
    public void trackEvent(Long userId, Long subscriptionId, SupportEmailEventRequest request) {
        if (request == null) {
            throw new ApiException("request is required");
        }
        if (request.action() == null) {
            throw new ApiException("action is required");
        }
        if (request.event() == null) {
            throw new ApiException("event is required");
        }

        Subscription subscription = requireOwnedSubscription(userId, subscriptionId);
        SupportEmailEvent event = new SupportEmailEvent();
        event.setSubscription(subscription);
        event.setUser(subscription.getUser());
        event.setAction(request.action());
        event.setEventType(request.event());
        event.setProvider(resolveProvider(subscription));
        supportEmailEventRepository.save(event);
    }

    private Subscription requireOwnedSubscription(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found"));

        Long ownerId = subscription.getUser() == null ? null : subscription.getUser().getId();
        if (ownerId == null || !ownerId.equals(userId)) {
            throw new SubscriptionOwnershipException("Subscription does not belong to current user");
        }

        return subscription;
    }

    private Map<String, String> buildPlaceholders(Subscription subscription, User user) {
        String serviceName = sanitizeToken(subscription.getServiceName(), MAX_SERVICE_NAME_LENGTH);
        String amount = subscription.getAmount() == null
                ? ""
                : sanitizeToken(subscription.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(), MAX_AMOUNT_LENGTH);
        String currency = sanitizeToken(subscription.getCurrency(), MAX_CURRENCY_LENGTH).toUpperCase(Locale.ROOT);
        String billingPeriod = subscription.getBillingPeriod() == null
                ? ""
                : sanitizeToken(subscription.getBillingPeriod().name(), MAX_BILLING_PERIOD_LENGTH).toLowerCase(Locale.ROOT);
        String nextBillingDate = subscription.getNextBillingDate() == null
                ? ""
                : sanitizeToken(subscription.getNextBillingDate().toString(), MAX_DATE_LENGTH);
        String accountEmail = user == null ? "" : sanitizeToken(user.getEmail(), MAX_ACCOUNT_EMAIL_LENGTH);

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("{serviceName}", serviceName);
        placeholders.put("{amount}", amount);
        placeholders.put("{currency}", currency);
        placeholders.put("{billingPeriod}", billingPeriod);
        placeholders.put("{nextBillingDate}", nextBillingDate);
        placeholders.put("{accountEmail}", accountEmail);
        placeholders.put("{userName}", "");
        return placeholders;
    }

    private String resolveProvider(Subscription subscription) {
        SourceType sourceType = subscription.getSourceType();
        if (sourceType == null) {
            return "UNKNOWN";
        }

        return switch (sourceType) {
            case MANUAL -> "MANUAL";
            case MAIL_IMPORT -> ImportProvider.GMAIL.name();
            case BANK_IMPORT -> ImportProvider.BANK_API.name();
        };
    }

    private String applyTemplate(String template, Map<String, String> placeholders) {
        String value = template == null ? "" : template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace(entry.getKey(), entry.getValue());
        }
        return value;
    }

    private String sanitizeSubject(String subject) {
        String sanitized = sanitizeToken(subject, MAX_SUBJECT_LENGTH);
        return sanitized.isBlank() ? DEFAULT_SUBJECT : sanitized;
    }

    private String sanitizeBody(String body) {
        String normalized = body == null ? "" : body;
        normalized = normalized.replace("\r\n", "\n").replace('\r', '\n');
        normalized = normalized.replace("\u0000", "");
        normalized = truncate(normalized, MAX_BODY_LENGTH);
        return normalized.trim();
    }

    private String sanitizeToken(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        String sanitized = value.trim()
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
        sanitized = CONTROL_CHARS_PATTERN.matcher(sanitized).replaceAll(" ");
        sanitized = MULTI_SPACE_PATTERN.matcher(sanitized).replaceAll(" ").trim();
        return truncate(sanitized, maxLength);
    }

    private String sanitizeEmail(String email) {
        String sanitized = sanitizeToken(email, MAX_ACCOUNT_EMAIL_LENGTH).toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(sanitized).matches()) {
            return templateRegistry.defaultSupportEmail();
        }
        return sanitized;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String buildMailtoUrl(String to, String subject, String body) {
        return "mailto:" + to
                + "?subject=" + encode(subject)
                + "&body=" + encode(body);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

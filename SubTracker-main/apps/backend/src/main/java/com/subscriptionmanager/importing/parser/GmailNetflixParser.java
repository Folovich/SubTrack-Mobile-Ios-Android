package com.subscriptionmanager.importing.parser;

import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.importing.dto.MailMessageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GmailNetflixParser {
    private static final String DEFAULT_CURRENCY = "USD";

    private static final List<SupportedTemplate> SUPPORTED_TEMPLATES = List.of(
            new SupportedTemplate("Netflix", "Entertainment", List.of("netflix")),
            new SupportedTemplate("Spotify", "Entertainment", List.of("spotify")),
            new SupportedTemplate("YouTube Premium", "Entertainment", List.of("youtube premium", "youtube")),
            new SupportedTemplate("Google One", "Cloud Storage", List.of("google one")),
            new SupportedTemplate("ChatGPT", "AI Tools", List.of("chatgpt", "openai")),
            new SupportedTemplate("Notion", "Productivity", List.of("notion")),
            new SupportedTemplate("Slack", "Productivity", List.of("slack")),
            new SupportedTemplate("Dropbox", "Cloud Storage", List.of("dropbox")),
            new SupportedTemplate("Adobe", "Productivity", List.of("adobe", "creative cloud")),
            new SupportedTemplate("Canva", "Design", List.of("canva"))
    );

    private static final List<String> BILLING_KEYWORDS = List.of(
            "billing",
            "renewal",
            "payment",
            "invoice",
            "receipt",
            "subscription",
            "charged",
            "plan",
            "next payment",
            "upcoming payment"
    );

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ENGLISH)
    );

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b(20\\d{2}-\\d{2}-\\d{2})\\b");
    private static final Pattern TEXTUAL_DATE_PATTERN = Pattern.compile(
            "\\b([A-Z]{3,9}\\s+\\d{1,2}(?:st|nd|rd|th)?,\\s+20\\d{2}|\\d{1,2}(?:st|nd|rd|th)?\\s+[A-Z]{3,9}\\s+20\\d{2}|\\d{1,2}/\\d{1,2}/20\\d{2}|\\d{1,2}\\.\\d{1,2}\\.20\\d{2})\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern AMOUNT_WITH_CURRENCY_PATTERN = Pattern.compile(
            "(?i)(?:amount|charge(?:d)?|renewal|payment|total|invoice|receipt|plan)\\D{0,20}(\\d+(?:[.,]\\d{1,2})?)\\s*([A-Z]{3})"
    );
    private static final Pattern CURRENCY_BEFORE_AMOUNT_PATTERN =
            Pattern.compile("(?i)\\b([A-Z]{3})\\s*(\\d+(?:[.,]\\d{1,2})?)\\b");
    private static final Pattern SYMBOL_AMOUNT_PATTERN =
            Pattern.compile("([€£$₽])\\s*(\\d+(?:[.,]\\d{1,2})?)");
    private static final Pattern FROM_DOMAIN_PATTERN =
            Pattern.compile("@([A-Z0-9.-]+)", Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> SYMBOL_TO_CURRENCY = Map.of(
            "$", "USD",
            "€", "EUR",
            "£", "GBP",
            "₽", "RUB"
    );

    public ParseResult parse(MailMessageRequest message) {
        String merged = (message.subject() + "\n" + message.body()).trim();
        String normalized = merged.toLowerCase(Locale.ROOT);
        String normalizedFrom = message.from().toLowerCase(Locale.ROOT);

        if (!looksLikeSubscriptionMail(normalized, normalizedFrom)) {
            return ParseResult.unsupported("unsupported template: message does not contain subscription billing signals");
        }

        SupportedTemplate template = detectTemplate(normalized, normalizedFrom);
        String serviceName = template != null ? template.serviceName() : inferServiceNameFromSender(normalizedFrom);
        if (serviceName == null) {
            return ParseResult.unsupported("unsupported template: service name was not recognized");
        }

        LocalDate nextBillingDate = parseDate(merged)
                .orElse(null);
        if (nextBillingDate == null) {
            return ParseResult.parseError("parser could not extract next billing date");
        }

        AmountWithCurrency amountWithCurrency = parseAmount(merged)
                .orElse(null);
        if (amountWithCurrency == null || amountWithCurrency.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ParseResult.parseError("parser could not extract valid amount");
        }

        BillingPeriod billingPeriod = detectBillingPeriod(normalized);
        String category = template != null ? template.categoryName() : defaultCategory(serviceName);
        String sourceProvider = inferSourceProvider(normalizedFrom, serviceName);

        ParsedSubscription parsedSubscription = new ParsedSubscription(
                serviceName,
                amountWithCurrency.amount(),
                amountWithCurrency.currency(),
                billingPeriod,
                nextBillingDate,
                category,
                sourceProvider
        );
        return ParseResult.success(parsedSubscription);
    }

    private boolean looksLikeSubscriptionMail(String text, String from) {
        return BILLING_KEYWORDS.stream().anyMatch(text::contains) || BILLING_KEYWORDS.stream().anyMatch(from::contains);
    }

    private SupportedTemplate detectTemplate(String text, String from) {
        for (SupportedTemplate template : SUPPORTED_TEMPLATES) {
            for (String marker : template.markers()) {
                if (text.contains(marker) || from.contains(marker)) {
                    return template;
                }
            }
        }
        return null;
    }

    private String inferServiceNameFromSender(String from) {
        Matcher matcher = FROM_DOMAIN_PATTERN.matcher(from);
        if (!matcher.find()) {
            return null;
        }

        String[] parts = matcher.group(1).split("\\.");
        for (String part : parts) {
            if (!part.isBlank() && !List.of("mail", "billing", "notifications", "notify", "team", "support", "noreply").contains(part)) {
                return Character.toUpperCase(part.charAt(0)) + part.substring(1);
            }
        }
        return null;
    }

    private String inferSourceProvider(String from, String serviceName) {
        Matcher matcher = FROM_DOMAIN_PATTERN.matcher(from);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase(Locale.ROOT);
        }
        return serviceName.toLowerCase(Locale.ROOT);
    }

    private String defaultCategory(String serviceName) {
        String normalized = serviceName.toLowerCase(Locale.ROOT);
        if (normalized.contains("cloud") || normalized.contains("dropbox") || normalized.contains("google")) {
            return "Cloud Storage";
        }
        if (normalized.contains("adobe") || normalized.contains("slack") || normalized.contains("notion")) {
            return "Productivity";
        }
        if (normalized.contains("chatgpt") || normalized.contains("openai")) {
            return "AI Tools";
        }
        return "Subscriptions";
    }

    private Optional<LocalDate> parseDate(String text) {
        Matcher isoMatcher = ISO_DATE_PATTERN.matcher(text);
        if (isoMatcher.find()) {
            return tryDate(isoMatcher.group(1));
        }

        Matcher textualMatcher = TEXTUAL_DATE_PATTERN.matcher(text);
        if (!textualMatcher.find()) {
            return Optional.empty();
        }

        return tryDate(textualMatcher.group(1));
    }

    private Optional<LocalDate> tryDate(String rawDate) {
        String sanitized = sanitizeDateToken(rawDate);
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(sanitized, formatter));
            } catch (DateTimeParseException ignored) {
                // Try next formatter.
            }
        }

        String normalized = normalizeMonthCase(sanitized);
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(normalized, formatter));
            } catch (DateTimeParseException ignored) {
                // Try next formatter.
            }
        }
        return Optional.empty();
    }

    private String sanitizeDateToken(String rawDate) {
        return rawDate.replaceAll("(?i)(\\d{1,2})(st|nd|rd|th)", "$1");
    }

    private String normalizeMonthCase(String rawDate) {
        for (java.time.Month month : java.time.Month.values()) {
            String full = month.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            String shortName = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            rawDate = rawDate.replaceAll("(?i)" + full, full);
            rawDate = rawDate.replaceAll("(?i)" + shortName, shortName);
        }
        return rawDate;
    }

    private Optional<AmountWithCurrency> parseAmount(String text) {
        Matcher amountCurrencyMatcher = AMOUNT_WITH_CURRENCY_PATTERN.matcher(text);
        if (amountCurrencyMatcher.find()) {
            BigDecimal amount = parseDecimal(amountCurrencyMatcher.group(1));
            if (amount != null) {
                return Optional.of(new AmountWithCurrency(amount, amountCurrencyMatcher.group(2).toUpperCase(Locale.ROOT)));
            }
        }

        Matcher currencyBeforeAmountMatcher = CURRENCY_BEFORE_AMOUNT_PATTERN.matcher(text);
        if (currencyBeforeAmountMatcher.find()) {
            BigDecimal amount = parseDecimal(currencyBeforeAmountMatcher.group(2));
            if (amount != null) {
                return Optional.of(new AmountWithCurrency(amount, currencyBeforeAmountMatcher.group(1).toUpperCase(Locale.ROOT)));
            }
        }

        Matcher symbolMatcher = SYMBOL_AMOUNT_PATTERN.matcher(text);
        if (symbolMatcher.find()) {
            BigDecimal amount = parseDecimal(symbolMatcher.group(2));
            if (amount != null) {
                String currency = SYMBOL_TO_CURRENCY.getOrDefault(symbolMatcher.group(1), DEFAULT_CURRENCY);
                return Optional.of(new AmountWithCurrency(amount, currency));
            }
        }

        return Optional.empty();
    }

    private BillingPeriod detectBillingPeriod(String text) {
        if (text.contains("annual") || text.contains("yearly") || text.contains("per year") || text.contains("12 months")) {
            return BillingPeriod.YEARLY;
        }
        if (text.contains("quarter") || text.contains("every 3 months")) {
            return BillingPeriod.QUARTERLY;
        }
        if (text.contains("weekly") || text.contains("per week")) {
            return BillingPeriod.WEEKLY;
        }
        return BillingPeriod.MONTHLY;
    }

    private BigDecimal parseDecimal(String raw) {
        try {
            return new BigDecimal(raw.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record AmountWithCurrency(BigDecimal amount, String currency) {
    }

    private record SupportedTemplate(String serviceName, String categoryName, List<String> markers) {
    }

    public record ParsedSubscription(
            String serviceName,
            BigDecimal amount,
            String currency,
            BillingPeriod billingPeriod,
            LocalDate nextBillingDate,
            String categoryName,
            String sourceProvider
    ) {
    }

    public enum ParseStatus {
        SUCCESS,
        UNSUPPORTED,
        PARSE_ERROR
    }

    public record ParseResult(
            ParseStatus status,
            ParsedSubscription parsedSubscription,
            String reason
    ) {
        public boolean success() {
            return status == ParseStatus.SUCCESS;
        }

        public boolean unsupported() {
            return status == ParseStatus.UNSUPPORTED;
        }

        public static ParseResult success(ParsedSubscription parsedSubscription) {
            return new ParseResult(ParseStatus.SUCCESS, parsedSubscription, null);
        }

        public static ParseResult unsupported(String reason) {
            return new ParseResult(ParseStatus.UNSUPPORTED, null, reason);
        }

        public static ParseResult parseError(String reason) {
            return new ParseResult(ParseStatus.PARSE_ERROR, null, reason);
        }
    }
}

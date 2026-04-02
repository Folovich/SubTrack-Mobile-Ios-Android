package com.subscriptionmanager.importing.parser;

import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.importing.dto.MailMessageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GmailNetflixParser {
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String ENTERTAINMENT_CATEGORY = "Entertainment";
    private static final String MEMBERSHIP_CATEGORY = "Membership / Subscription";
    private static final BigDecimal RUB_TO_USD_RATE = new BigDecimal("79.89");
    private static final Locale RUSSIAN_LOCALE = Locale.forLanguageTag("ru");
    private static final String CURRENCY_TOKEN_PATTERN =
            "USD|RUB|EUR|GBP|\\$|€|£|₽|руб(?:\\.|ля|лей|ль)?|р\\.?";
    private static final String BILLING_SIGNAL_PATTERN =
            "billing|renewal|payment|invoice|receipt|subscription|charged|plan|next payment|upcoming payment"
                    + "|подписка|продление|списание|оплата|платеж|платёж|следующее списание"
                    + "|дата следующего платежа|чек|квитанция|сумма";

    private static final List<SupportedTemplate> SUPPORTED_TEMPLATES = List.of(
            new SupportedTemplate("Netflix", ENTERTAINMENT_CATEGORY, List.of("netflix")),
            new SupportedTemplate("Spotify", ENTERTAINMENT_CATEGORY, List.of("spotify")),
            new SupportedTemplate("YouTube Premium", ENTERTAINMENT_CATEGORY, List.of("youtube premium", "youtube")),
            new SupportedTemplate("Google One", "Cloud Storage", List.of("google one")),
            new SupportedTemplate("ChatGPT", "AI Tools", List.of("chatgpt", "openai")),
            new SupportedTemplate("Notion", "Productivity", List.of("notion")),
            new SupportedTemplate("Slack", "Productivity", List.of("slack")),
            new SupportedTemplate("Dropbox", "Cloud Storage", List.of("dropbox")),
            new SupportedTemplate("Adobe", "Productivity", List.of("adobe", "creative cloud")),
            new SupportedTemplate("Canva", "Design", List.of("canva")),
            new SupportedTemplate("Yandex Music", ENTERTAINMENT_CATEGORY, List.of("yandex music", "яндекс музыка", "music.yandex")),
            new SupportedTemplate("Kinopoisk", ENTERTAINMENT_CATEGORY, List.of("kinopoisk", "кинопоиск")),
            new SupportedTemplate("IVI", ENTERTAINMENT_CATEGORY, List.of("ivi", "иви", "ivi.ru")),
            new SupportedTemplate("Okko", ENTERTAINMENT_CATEGORY, List.of("okko", "окко")),
            new SupportedTemplate("VK Music", ENTERTAINMENT_CATEGORY, List.of("vk music", "vk музыка", "вк музыка", "music.vk")),
            new SupportedTemplate("SberPrime", MEMBERSHIP_CATEGORY, List.of("sberprime", "sber prime", "сберпрайм", "сбер прайм")),
            new SupportedTemplate("Yandex Plus", MEMBERSHIP_CATEGORY, List.of("yandex plus", "яндекс плюс", "plus.yandex")),
            new SupportedTemplate("KION", ENTERTAINMENT_CATEGORY, List.of("kion")),
            new SupportedTemplate("Premier", ENTERTAINMENT_CATEGORY, List.of("premier", "премьер", "premier.one")),
            new SupportedTemplate("Wink", ENTERTAINMENT_CATEGORY, List.of("wink")),
            new SupportedTemplate("Tinkoff Pro", MEMBERSHIP_CATEGORY, List.of("tinkoff pro", "тинькофф pro", "тинькофф про"))
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
            "upcoming payment",
            "подписка",
            "продление",
            "списание",
            "оплата",
            "платеж",
            "платёж",
            "следующее списание",
            "дата следующего платежа",
            "чек",
            "квитанция",
            "сумма"
    );

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM uuuu", RUSSIAN_LOCALE),
            DateTimeFormatter.ofPattern("d MMM uuuu", RUSSIAN_LOCALE),
            DateTimeFormatter.ofPattern("M/d/uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d.M.uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ENGLISH)
    );

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b(20\\d{2}-\\d{2}-\\d{2})\\b");
    private static final Pattern NUMERIC_DATE_PATTERN = Pattern.compile("\\b(\\d{1,2}[./]\\d{1,2}[./]20\\d{2})\\b");
    private static final Pattern TEXTUAL_DATE_PATTERN = Pattern.compile(
            "(?iu)\\b([\\p{L}]{3,12}\\s+\\d{1,2}(?:st|nd|rd|th)?,\\s*20\\d{2}|\\d{1,2}(?:st|nd|rd|th)?\\s+[\\p{L}]{3,12}\\.?\\s+20\\d{2})\\b"
    );
    private static final Pattern DAY_FIRST_TEXTUAL_DATE_PATTERN =
            Pattern.compile("(?iu)^(\\d{1,2})\\s+([\\p{L}]+)\\s+(20\\d{2})$");
    private static final Pattern MONTH_FIRST_TEXTUAL_DATE_PATTERN =
            Pattern.compile("(?iu)^([\\p{L}]+)\\s+(\\d{1,2})\\s+(20\\d{2})$");
    private static final Pattern CONTEXTUAL_AMOUNT_AFTER_PATTERN = Pattern.compile(
            "(?iu)(?:" + BILLING_SIGNAL_PATTERN + ")\\D{0,25}(\\d+(?:[.,]\\d{1,2})?)\\s*(" + CURRENCY_TOKEN_PATTERN + ")"
    );
    private static final Pattern CONTEXTUAL_AMOUNT_BEFORE_PATTERN = Pattern.compile(
            "(?iu)(?:" + BILLING_SIGNAL_PATTERN + ")\\D{0,25}(" + CURRENCY_TOKEN_PATTERN + ")\\s*(\\d+(?:[.,]\\d{1,2})?)"
    );
    private static final Pattern AMOUNT_WITH_CURRENCY_PATTERN = Pattern.compile(
            "(?iu)(\\d+(?:[.,]\\d{1,2})?)\\s*(" + CURRENCY_TOKEN_PATTERN + ")"
    );
    private static final Pattern CURRENCY_BEFORE_AMOUNT_PATTERN = Pattern.compile(
            "(?iu)(" + CURRENCY_TOKEN_PATTERN + ")\\s*(\\d+(?:[.,]\\d{1,2})?)"
    );
    private static final Pattern FROM_DOMAIN_PATTERN =
            Pattern.compile("@([A-Z0-9.-]+)", Pattern.CASE_INSENSITIVE);

    private static final Map<String, String> CURRENCY_ALIASES = Map.ofEntries(
            Map.entry("usd", "USD"),
            Map.entry("$", "USD"),
            Map.entry("eur", "EUR"),
            Map.entry("€", "EUR"),
            Map.entry("gbp", "GBP"),
            Map.entry("£", "GBP"),
            Map.entry("rub", "RUB"),
            Map.entry("руб", "RUB"),
            Map.entry("руб.", "RUB"),
            Map.entry("рубль", "RUB"),
            Map.entry("рубля", "RUB"),
            Map.entry("рублей", "RUB"),
            Map.entry("р", "RUB"),
            Map.entry("р.", "RUB"),
            Map.entry("₽", "RUB")
    );

    private static final Map<String, Integer> MONTH_ALIASES = Map.ofEntries(
            Map.entry("january", 1), Map.entry("jan", 1), Map.entry("январь", 1), Map.entry("января", 1), Map.entry("янв", 1),
            Map.entry("february", 2), Map.entry("feb", 2), Map.entry("февраль", 2), Map.entry("февраля", 2), Map.entry("фев", 2),
            Map.entry("march", 3), Map.entry("mar", 3), Map.entry("март", 3), Map.entry("марта", 3), Map.entry("мар", 3),
            Map.entry("april", 4), Map.entry("apr", 4), Map.entry("апрель", 4), Map.entry("апреля", 4), Map.entry("апр", 4),
            Map.entry("may", 5), Map.entry("май", 5), Map.entry("мая", 5),
            Map.entry("june", 6), Map.entry("jun", 6), Map.entry("июнь", 6), Map.entry("июня", 6), Map.entry("июн", 6),
            Map.entry("july", 7), Map.entry("jul", 7), Map.entry("июль", 7), Map.entry("июля", 7), Map.entry("июл", 7),
            Map.entry("august", 8), Map.entry("aug", 8), Map.entry("август", 8), Map.entry("августа", 8), Map.entry("авг", 8),
            Map.entry("september", 9), Map.entry("sep", 9), Map.entry("sept", 9), Map.entry("сентябрь", 9), Map.entry("сентября", 9), Map.entry("сен", 9),
            Map.entry("october", 10), Map.entry("oct", 10), Map.entry("октябрь", 10), Map.entry("октября", 10), Map.entry("окт", 10),
            Map.entry("november", 11), Map.entry("nov", 11), Map.entry("ноябрь", 11), Map.entry("ноября", 11), Map.entry("ноя", 11),
            Map.entry("december", 12), Map.entry("dec", 12), Map.entry("декабрь", 12), Map.entry("декабря", 12), Map.entry("дек", 12)
    );

    public ParseResult parse(MailMessageRequest message) {
        String subject = message.subject() == null ? "" : message.subject();
        String body = message.body() == null ? "" : message.body();
        String merged = (subject + "\n" + body).trim();
        String normalized = merged.toLowerCase(Locale.ROOT);
        String normalizedFrom = message.from() == null ? "" : message.from().toLowerCase(Locale.ROOT);
        SupportedTemplate template = detectTemplate(normalized, normalizedFrom);

        if (template == null && !looksLikeSubscriptionMail(normalized, normalizedFrom)) {
            return ParseResult.unsupported("unsupported template: message does not contain subscription billing signals");
        }

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
        if (normalized.contains("music")
                || normalized.contains("kinopoisk")
                || normalized.contains("ivi")
                || normalized.contains("okko")
                || normalized.contains("kion")
                || normalized.contains("premier")
                || normalized.contains("wink")) {
            return ENTERTAINMENT_CATEGORY;
        }
        if (normalized.contains("plus")
                || normalized.contains("prime")
                || normalized.contains("pro")
                || normalized.contains("тинькофф")
                || normalized.contains("сбер")) {
            return MEMBERSHIP_CATEGORY;
        }
        return "Subscriptions";
    }

    private Optional<LocalDate> parseDate(String text) {
        Matcher isoMatcher = ISO_DATE_PATTERN.matcher(text);
        if (isoMatcher.find()) {
            return tryDate(isoMatcher.group(1));
        }

        Matcher numericMatcher = NUMERIC_DATE_PATTERN.matcher(text);
        if (numericMatcher.find()) {
            return tryDate(numericMatcher.group(1));
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

        return parseTextualDate(sanitized);
    }

    private String sanitizeDateToken(String rawDate) {
        return rawDate
                .replaceAll("(?i)(\\d{1,2})(st|nd|rd|th)", "$1")
                .replaceAll("(?iu)([\\p{L}]{3,12})\\.", "$1")
                .replace(',', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Optional<LocalDate> parseTextualDate(String rawDate) {
        String normalized = rawDate.toLowerCase(Locale.ROOT);

        Matcher dayFirstMatcher = DAY_FIRST_TEXTUAL_DATE_PATTERN.matcher(normalized);
        if (dayFirstMatcher.matches()) {
            return buildTextualDate(dayFirstMatcher.group(1), dayFirstMatcher.group(2), dayFirstMatcher.group(3));
        }

        Matcher monthFirstMatcher = MONTH_FIRST_TEXTUAL_DATE_PATTERN.matcher(normalized);
        if (monthFirstMatcher.matches()) {
            return buildTextualDate(monthFirstMatcher.group(2), monthFirstMatcher.group(1), monthFirstMatcher.group(3));
        }

        return Optional.empty();
    }

    private Optional<LocalDate> buildTextualDate(String dayRaw, String monthRaw, String yearRaw) {
        Integer month = MONTH_ALIASES.get(monthRaw);
        if (month == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDate.of(
                    Integer.parseInt(yearRaw),
                    month,
                    Integer.parseInt(dayRaw)
            ));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private Optional<AmountWithCurrency> parseAmount(String text) {
        Matcher contextualAmountMatcher = CONTEXTUAL_AMOUNT_AFTER_PATTERN.matcher(text);
        if (contextualAmountMatcher.find()) {
            return buildAmountWithCurrency(contextualAmountMatcher.group(1), contextualAmountMatcher.group(2));
        }

        Matcher contextualCurrencyMatcher = CONTEXTUAL_AMOUNT_BEFORE_PATTERN.matcher(text);
        if (contextualCurrencyMatcher.find()) {
            return buildAmountWithCurrency(contextualCurrencyMatcher.group(2), contextualCurrencyMatcher.group(1));
        }

        Matcher amountCurrencyMatcher = AMOUNT_WITH_CURRENCY_PATTERN.matcher(text);
        if (amountCurrencyMatcher.find()) {
            return buildAmountWithCurrency(amountCurrencyMatcher.group(1), amountCurrencyMatcher.group(2));
        }

        Matcher currencyBeforeAmountMatcher = CURRENCY_BEFORE_AMOUNT_PATTERN.matcher(text);
        if (currencyBeforeAmountMatcher.find()) {
            return buildAmountWithCurrency(currencyBeforeAmountMatcher.group(2), currencyBeforeAmountMatcher.group(1));
        }

        return Optional.empty();
    }

    private BillingPeriod detectBillingPeriod(String text) {
        if (text.contains("annual")
                || text.contains("yearly")
                || text.contains("per year")
                || text.contains("12 months")
                || text.contains("ежегод")
                || text.contains("годов")) {
            return BillingPeriod.YEARLY;
        }
        if (text.contains("quarter")
                || text.contains("every 3 months")
                || text.contains("квартал")
                || text.contains("каждые 3 месяца")) {
            return BillingPeriod.QUARTERLY;
        }
        if (text.contains("weekly")
                || text.contains("per week")
                || text.contains("еженедел")
                || text.contains("каждую неделю")) {
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

    private Optional<AmountWithCurrency> buildAmountWithCurrency(String rawAmount, String rawCurrency) {
        BigDecimal amount = parseDecimal(rawAmount);
        if (amount == null) {
            return Optional.empty();
        }

        String normalizedCurrency = normalizeCurrency(rawCurrency);
        if (normalizedCurrency == null) {
            return Optional.empty();
        }

        if ("RUB".equals(normalizedCurrency)) {
            BigDecimal usdAmount = amount.divide(RUB_TO_USD_RATE, 2, RoundingMode.HALF_UP);
            return Optional.of(new AmountWithCurrency(usdAmount, DEFAULT_CURRENCY));
        }

        return Optional.of(new AmountWithCurrency(amount, normalizedCurrency));
    }

    private String normalizeCurrency(String rawCurrency) {
        if (rawCurrency == null) {
            return null;
        }
        return CURRENCY_ALIASES.get(rawCurrency.trim().toLowerCase(Locale.ROOT));
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

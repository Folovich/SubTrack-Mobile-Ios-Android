package com.subscriptionmanager.importing.parser;

import com.subscriptionmanager.importing.dto.MailMessageRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GmailNetflixParserTest {

    private static final BigDecimal RUB_TO_USD_RATE = new BigDecimal("79.89");

    private final GmailNetflixParser parser = new GmailNetflixParser();

    @Test
    void parseSuccessForNetflixTemplate() {
        MailMessageRequest message = new MailMessageRequest(
                "ext-1",
                "billing@netflix.com",
                "Netflix renewal notice",
                "Your Netflix renewal is scheduled for 2026-03-20. Amount: 9.99 USD",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult result = parser.parse(message);

        assertTrue(result.success());
        assertEquals("Netflix", result.parsedSubscription().serviceName());
        assertEquals(BigDecimal.valueOf(9.99).setScale(2), result.parsedSubscription().amount());
        assertEquals("USD", result.parsedSubscription().currency());
        assertEquals(LocalDate.of(2026, 3, 20), result.parsedSubscription().nextBillingDate());
    }

    @Test
    void parseFailsWhenDateMissing() {
        MailMessageRequest message = new MailMessageRequest(
                "ext-2",
                "billing@netflix.com",
                "Netflix renewal notice",
                "Amount: 9.99 USD",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult result = parser.parse(message);

        assertFalse(result.success());
        assertTrue(result.reason().contains("next billing date"));
    }

    @Test
    void parseSuccessForSpotifyTemplate() {
        MailMessageRequest message = new MailMessageRequest(
                "ext-3",
                "billing@spotify.com",
                "Spotify Premium renewal",
                "Spotify charged 5.99 USD. Next billing date: 2026-04-01",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult result = parser.parse(message);

        assertTrue(result.success());
        assertEquals("Spotify", result.parsedSubscription().serviceName());
        assertEquals(BigDecimal.valueOf(5.99).setScale(2), result.parsedSubscription().amount());
        assertEquals(LocalDate.of(2026, 4, 1), result.parsedSubscription().nextBillingDate());
    }

    @Test
    void parseSuccessForYoutubeTemplateWithDollarAmount() {
        MailMessageRequest message = new MailMessageRequest(
                "ext-4",
                "no-reply@youtube.com",
                "YouTube Premium charge",
                "Your YouTube Premium renewal is on 2026-05-15. Charged $12.99",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult result = parser.parse(message);

        assertTrue(result.success());
        assertEquals("YouTube Premium", result.parsedSubscription().serviceName());
        assertEquals(BigDecimal.valueOf(12.99).setScale(2), result.parsedSubscription().amount());
        assertEquals(LocalDate.of(2026, 5, 15), result.parsedSubscription().nextBillingDate());
    }

    @Test
    void parseSuccessForOrdinalDateFormat() {
        MailMessageRequest message = new MailMessageRequest(
                "ext-5",
                "noreply@tm.openai.com",
                "Your Plus access will end soon",
                "Your ChatGPT subscription renewal is scheduled for March 14th, 2026. Amount: 20.00 USD",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult result = parser.parse(message);

        assertTrue(result.success());
        assertEquals("ChatGPT", result.parsedSubscription().serviceName());
        assertEquals(BigDecimal.valueOf(20.00).setScale(2), result.parsedSubscription().amount());
        assertEquals(LocalDate.of(2026, 3, 14), result.parsedSubscription().nextBillingDate());
    }

    @Test
    void parseSuccessForDotSeparatedDateFormat() {
        MailMessageRequest message = new MailMessageRequest(
                "ext-6",
                "billing@spotify.com",
                "Spotify Premium renewal",
                "Spotify charged 5.99 USD. Next billing date: 14.04.2026",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult result = parser.parse(message);

        assertTrue(result.success());
        assertEquals("Spotify", result.parsedSubscription().serviceName());
        assertEquals(BigDecimal.valueOf(5.99).setScale(2), result.parsedSubscription().amount());
        assertEquals(LocalDate.of(2026, 4, 14), result.parsedSubscription().nextBillingDate());
    }

    @Test
    void parseRussianYandexMusicMailConvertsRublesToUsd() {
        MailMessageRequest message = new MailMessageRequest(
                "ru-1",
                "noreply@music.yandex.ru",
                "Яндекс Музыка: продление подписки",
                "Подписка Яндекс Музыка продлена. Следующее списание: 20 марта 2026. Сумма: 299,00 ₽",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult result = parser.parse(message);

        assertTrue(result.success());
        assertEquals("Yandex Music", result.parsedSubscription().serviceName());
        assertEquals("USD", result.parsedSubscription().currency());
        assertEquals(rubToUsd("299.00"), result.parsedSubscription().amount());
        assertEquals(LocalDate.of(2026, 3, 20), result.parsedSubscription().nextBillingDate());
        assertEquals("Entertainment", result.parsedSubscription().categoryName());
    }

    @Test
    void parseRussianAmountWithRubleSymbolBeforeNumber() {
        MailMessageRequest message = new MailMessageRequest(
                "ru-2",
                "billing@kinopoisk.ru",
                "Кинопоиск: продление",
                "Подписка Кинопоиск активна. Дата следующего платежа: 2026-03-20. К оплате: ₽299",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult result = parser.parse(message);

        assertTrue(result.success());
        assertEquals("Kinopoisk", result.parsedSubscription().serviceName());
        assertEquals(rubToUsd("299.00"), result.parsedSubscription().amount());
        assertEquals("USD", result.parsedSubscription().currency());
    }

    @Test
    void parseRussianAmountWithRubWordAndSlashDate() {
        MailMessageRequest message = new MailMessageRequest(
                "ru-3",
                "info@ivi.ru",
                "Иви: оплата подписки",
                "Подписка Иви продлена. Следующее списание 20/03/2026. Оплата: 299 руб",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult result = parser.parse(message);

        assertTrue(result.success());
        assertEquals("IVI", result.parsedSubscription().serviceName());
        assertEquals(rubToUsd("299.00"), result.parsedSubscription().amount());
        assertEquals(LocalDate.of(2026, 3, 20), result.parsedSubscription().nextBillingDate());
    }

    @Test
    void parseRussianAmountWithRubCodeAndShortMonth() {
        MailMessageRequest message = new MailMessageRequest(
                "ru-4",
                "plus@yandex.ru",
                "Yandex Plus receipt",
                "Яндекс Плюс: чек за подписку. Дата следующего платежа: 20 мар 2026. Сумма: 299,00 RUB",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult result = parser.parse(message);

        assertTrue(result.success());
        assertEquals("Yandex Plus", result.parsedSubscription().serviceName());
        assertEquals(rubToUsd("299.00"), result.parsedSubscription().amount());
        assertEquals("USD", result.parsedSubscription().currency());
        assertEquals(LocalDate.of(2026, 3, 20), result.parsedSubscription().nextBillingDate());
        assertEquals("Membership / Subscription", result.parsedSubscription().categoryName());
    }

    @Test
    void parseRussianAmountWithShortRubleNotation() {
        MailMessageRequest message = new MailMessageRequest(
                "ru-5",
                "billing@kion.ru",
                "KION: продление",
                "Подписка KION продление. Следующее списание 20.03.2026. Платеж: 299 р.",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult result = parser.parse(message);

        assertTrue(result.success());
        assertEquals("KION", result.parsedSubscription().serviceName());
        assertEquals(rubToUsd("299.00"), result.parsedSubscription().amount());
        assertEquals(LocalDate.of(2026, 3, 20), result.parsedSubscription().nextBillingDate());
    }

    @Test
    void parseCanonicalizesSberPrimeVariants() {
        MailMessageRequest compactName = new MailMessageRequest(
                "ru-6",
                "noreply@sberbank.ru",
                "СберПрайм: продление",
                "СберПрайм. Подписка продлена. Следующее списание: 20 марта 2026. Сумма: 299 ₽",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );
        MailMessageRequest spacedName = new MailMessageRequest(
                "ru-7",
                "noreply@sberbank.ru",
                "Сбер Прайм: продление",
                "Сбер Прайм. Подписка продлена. Следующее списание: 20 марта 2026. Сумма: 299 ₽",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult compactResult = parser.parse(compactName);
        GmailNetflixParser.ParseResult spacedResult = parser.parse(spacedName);

        assertTrue(compactResult.success());
        assertTrue(spacedResult.success());
        assertEquals("SberPrime", compactResult.parsedSubscription().serviceName());
        assertEquals("SberPrime", spacedResult.parsedSubscription().serviceName());
        assertEquals("Membership / Subscription", compactResult.parsedSubscription().categoryName());
        assertEquals("Membership / Subscription", spacedResult.parsedSubscription().categoryName());
    }

    @Test
    void parseReturnsParseErrorWhenRecognizedServiceHasNoAmount() {
        MailMessageRequest message = new MailMessageRequest(
                "ru-8",
                "noreply@sberbank.ru",
                "Сбер Прайм: продление",
                "Подписка Сбер Прайм продлена. Следующее списание: 20 марта 2026",
                OffsetDateTime.parse("2026-03-08T10:00:00Z")
        );

        GmailNetflixParser.ParseResult result = parser.parse(message);

        assertEquals(GmailNetflixParser.ParseStatus.PARSE_ERROR, result.status());
        assertTrue(result.reason().contains("valid amount"));
    }

    private BigDecimal rubToUsd(String rubAmount) {
        return new BigDecimal(rubAmount).divide(RUB_TO_USD_RATE, 2, RoundingMode.HALF_UP);
    }
}

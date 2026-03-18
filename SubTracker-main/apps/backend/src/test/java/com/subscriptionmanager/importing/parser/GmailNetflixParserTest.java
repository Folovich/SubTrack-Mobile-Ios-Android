package com.subscriptionmanager.importing.parser;

import com.subscriptionmanager.importing.dto.MailMessageRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GmailNetflixParserTest {

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
}

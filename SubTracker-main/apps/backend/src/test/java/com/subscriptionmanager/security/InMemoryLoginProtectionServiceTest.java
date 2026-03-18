package com.subscriptionmanager.security;

import com.subscriptionmanager.exception.LoginThrottledException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLoginProtectionServiceTest {

    private static final String EMAIL = "demo@subtrack.app";
    private static final String IP = "203.0.113.10";

    private MutableClock clock;
    private LoginProtectionProperties properties;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-03-16T08:00:00Z"), ZoneOffset.UTC);
        properties = new LoginProtectionProperties();
        properties.getRateLimit().setMaxAttempts(100);
        properties.getRateLimit().setWindow(Duration.ofMinutes(1));
        properties.getBruteForce().setMaxFailures(3);
        properties.getBruteForce().setCooldown(Duration.ofMinutes(5));
        properties.getBruteForce().setEntryTtl(Duration.ofHours(1));
    }

    @Test
    void rateLimitBlocksAfterConfiguredAttemptsPerWindow() {
        properties.getRateLimit().setMaxAttempts(2);
        InMemoryLoginProtectionService service = new InMemoryLoginProtectionService(properties, clock);

        service.validateLoginAttempt(EMAIL, IP);
        service.validateLoginAttempt(EMAIL, IP);

        LoginThrottledException exception = assertThrows(
                LoginThrottledException.class,
                () -> service.validateLoginAttempt(EMAIL, IP)
        );
        assertEquals(60, exception.getRetryAfterSeconds());

        clock.advance(Duration.ofSeconds(61));
        assertDoesNotThrow(() -> service.validateLoginAttempt(EMAIL, IP));
    }

    @Test
    void bruteForceLocksAfterSeriesOfFailuresForEmailAndIp() {
        InMemoryLoginProtectionService service = new InMemoryLoginProtectionService(properties, clock);

        service.validateLoginAttempt(EMAIL, IP);
        assertEquals(0, service.recordLoginFailure(EMAIL, IP));
        service.validateLoginAttempt(EMAIL, IP);
        assertEquals(0, service.recordLoginFailure(EMAIL, IP));
        service.validateLoginAttempt(EMAIL, IP);
        long retryAfter = service.recordLoginFailure(EMAIL, IP);

        assertEquals(300, retryAfter);

        LoginThrottledException blocked = assertThrows(
                LoginThrottledException.class,
                () -> service.validateLoginAttempt(EMAIL, IP)
        );
        assertTrue(blocked.getRetryAfterSeconds() >= 299);

        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        assertDoesNotThrow(() -> service.validateLoginAttempt(EMAIL, IP));
    }

    @Test
    void successfulLoginResetsFailureCounter() {
        InMemoryLoginProtectionService service = new InMemoryLoginProtectionService(properties, clock);

        assertEquals(0, service.recordLoginFailure(EMAIL, IP));
        assertEquals(0, service.recordLoginFailure(EMAIL, IP));

        service.recordLoginSuccess(EMAIL, IP);

        assertEquals(0, service.recordLoginFailure(EMAIL, IP));
        assertEquals(0, service.recordLoginFailure(EMAIL, IP));
        long retryAfter = service.recordLoginFailure(EMAIL, IP);

        assertEquals(300, retryAfter);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zoneId;

        private MutableClock(Instant instant, ZoneId zoneId) {
            this.instant = instant;
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}

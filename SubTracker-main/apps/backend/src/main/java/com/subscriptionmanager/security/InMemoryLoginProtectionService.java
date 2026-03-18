package com.subscriptionmanager.security;

import com.subscriptionmanager.exception.LoginThrottledException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InMemoryLoginProtectionService implements LoginProtectionService {

    private static final String UNKNOWN_IP = "unknown";
    private static final int CLEANUP_EVERY_REQUESTS = 100;

    private final LoginProtectionProperties properties;
    private final Clock clock;

    private final ConcurrentMap<String, RateLimitBucket> rateLimitBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BruteForceBucket> bruteForceBuckets = new ConcurrentHashMap<>();
    private final AtomicInteger requestCounter = new AtomicInteger(0);

    public InMemoryLoginProtectionService(LoginProtectionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void validateLoginAttempt(String email, String clientIp) {
        Instant now = clock.instant();
        cleanupStaleBuckets(now);

        String normalizedIp = normalizeIp(clientIp);
        Duration window = properties.getRateLimit().getWindow();
        int maxAttempts = properties.getRateLimit().getMaxAttempts();

        RateLimitBucket rateBucket = rateLimitBuckets.computeIfAbsent(normalizedIp, key -> new RateLimitBucket());
        long rateLimitRetryAfter = rateBucket.consumeOrRetryAfter(now, window, maxAttempts);
        if (rateLimitRetryAfter > 0) {
            throw new LoginThrottledException(rateLimitRetryAfter);
        }

        String bruteForceKey = bruteForceKey(email, normalizedIp);
        BruteForceBucket bruteForceBucket = bruteForceBuckets.computeIfAbsent(bruteForceKey, key -> new BruteForceBucket());
        long lockRetryAfter = bruteForceBucket.getRetryAfterIfLocked(now);
        if (lockRetryAfter > 0) {
            throw new LoginThrottledException(lockRetryAfter);
        }
    }

    @Override
    public long recordLoginFailure(String email, String clientIp) {
        Instant now = clock.instant();
        String key = bruteForceKey(email, normalizeIp(clientIp));
        BruteForceBucket bucket = bruteForceBuckets.computeIfAbsent(key, ignored -> new BruteForceBucket());
        return bucket.recordFailure(now, properties.getBruteForce().getMaxFailures(), properties.getBruteForce().getCooldown());
    }

    @Override
    public void recordLoginSuccess(String email, String clientIp) {
        Instant now = clock.instant();
        String key = bruteForceKey(email, normalizeIp(clientIp));
        BruteForceBucket bucket = bruteForceBuckets.get(key);
        if (bucket == null) {
            return;
        }
        bucket.reset(now);
        bruteForceBuckets.remove(key, bucket);
    }

    private void cleanupStaleBuckets(Instant now) {
        int current = requestCounter.incrementAndGet();
        if (current % CLEANUP_EVERY_REQUESTS != 0) {
            return;
        }

        Duration rateLimitTtl = properties.getRateLimit().getWindow().multipliedBy(2);
        Duration bruteForceTtl = properties.getBruteForce().getEntryTtl();

        for (Map.Entry<String, RateLimitBucket> entry : rateLimitBuckets.entrySet()) {
            if (entry.getValue().isExpired(now, rateLimitTtl)) {
                rateLimitBuckets.remove(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, BruteForceBucket> entry : bruteForceBuckets.entrySet()) {
            if (entry.getValue().isExpired(now, bruteForceTtl)) {
                bruteForceBuckets.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private static String bruteForceKey(String email, String normalizedIp) {
        return normalizeEmail(email) + "|" + normalizedIp;
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeIp(String ip) {
        if (ip == null) {
            return UNKNOWN_IP;
        }
        String normalized = ip.trim();
        return normalized.isEmpty() ? UNKNOWN_IP : normalized;
    }

    private static long retryAfterSeconds(Instant now, Instant until) {
        long millis = until.toEpochMilli() - now.toEpochMilli();
        if (millis <= 0) {
            return 1;
        }
        return (millis + 999L) / 1000L;
    }

    private static final class RateLimitBucket {
        private final Deque<Instant> attempts = new ArrayDeque<>();
        private Instant lastAccess = Instant.EPOCH;

        synchronized long consumeOrRetryAfter(Instant now, Duration window, int maxAttempts) {
            Instant cutoff = now.minus(window);
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
                attempts.pollFirst();
            }

            if (attempts.size() >= maxAttempts) {
                lastAccess = now;
                Instant oldest = attempts.peekFirst();
                return retryAfterSeconds(now, oldest.plus(window));
            }

            attempts.addLast(now);
            lastAccess = now;
            return 0;
        }

        synchronized boolean isExpired(Instant now, Duration ttl) {
            return lastAccess.plus(ttl).isBefore(now);
        }
    }

    private static final class BruteForceBucket {
        private int failures;
        private Instant lockUntil;
        private Instant lastAccess = Instant.EPOCH;

        synchronized long getRetryAfterIfLocked(Instant now) {
            if (lockUntil == null) {
                lastAccess = now;
                return 0;
            }

            if (!now.isBefore(lockUntil)) {
                lockUntil = null;
                failures = 0;
                lastAccess = now;
                return 0;
            }

            lastAccess = now;
            return retryAfterSeconds(now, lockUntil);
        }

        synchronized long recordFailure(Instant now, int maxFailures, Duration cooldown) {
            if (lockUntil != null && now.isBefore(lockUntil)) {
                lastAccess = now;
                return retryAfterSeconds(now, lockUntil);
            }

            if (lockUntil != null && !now.isBefore(lockUntil)) {
                lockUntil = null;
                failures = 0;
            }

            failures++;
            lastAccess = now;

            if (failures >= maxFailures) {
                failures = 0;
                lockUntil = now.plus(cooldown);
                return retryAfterSeconds(now, lockUntil);
            }

            return 0;
        }

        synchronized void reset(Instant now) {
            failures = 0;
            lockUntil = null;
            lastAccess = now;
        }

        synchronized boolean isExpired(Instant now, Duration ttl) {
            boolean unlocked = lockUntil == null || !now.isBefore(lockUntil);
            return unlocked && failures == 0 && lastAccess.plus(ttl).isBefore(now);
        }
    }
}

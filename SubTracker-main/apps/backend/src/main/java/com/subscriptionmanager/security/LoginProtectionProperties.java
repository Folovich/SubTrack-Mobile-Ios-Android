package com.subscriptionmanager.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@ConfigurationProperties(prefix = "app.security.login")
public class LoginProtectionProperties {

    @Valid
    private final RateLimit rateLimit = new RateLimit();

    @Valid
    private final BruteForce bruteForce = new BruteForce();

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public BruteForce getBruteForce() {
        return bruteForce;
    }

    public static class RateLimit {
        @Min(1)
        private int maxAttempts = 20;

        @NotNull
        private Duration window = Duration.ofMinutes(1);

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }

    public static class BruteForce {
        @Min(1)
        private int maxFailures = 5;

        @NotNull
        private Duration cooldown = Duration.ofMinutes(15);

        @NotNull
        private Duration entryTtl = Duration.ofHours(24);

        public int getMaxFailures() {
            return maxFailures;
        }

        public void setMaxFailures(int maxFailures) {
            this.maxFailures = maxFailures;
        }

        public Duration getCooldown() {
            return cooldown;
        }

        public void setCooldown(Duration cooldown) {
            this.cooldown = cooldown;
        }

        public Duration getEntryTtl() {
            return entryTtl;
        }

        public void setEntryTtl(Duration entryTtl) {
            this.entryTtl = entryTtl;
        }
    }
}

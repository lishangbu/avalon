package io.github.lishangbu.avalon.oauth2.authorizationserver.login;

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link LoginFailureTracker}.
 */
public class InMemoryLoginFailureTracker implements LoginFailureTracker {

    private final int maxFailures;
    private final Duration lockDuration;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public InMemoryLoginFailureTracker(Oauth2Properties properties) {
        int configuredMax = properties == null ? 0 : safeInt(properties.getMaxLoginFailures());
        this.maxFailures = Math.max(configuredMax, 0);
        Duration configuredDuration =
                properties == null ? null : properties.getLoginLockDuration();
        this.lockDuration =
                configuredDuration == null
                                || configuredDuration.isZero()
                                || configuredDuration.isNegative()
                        ? null
                        : configuredDuration;
    }

    @Override
    public boolean isEnabled() {
        return maxFailures > 0 && lockDuration != null;
    }

    @Override
    public Duration getRemainingLock(String username) {
        if (!isEnabled()) {
            return null;
        }
        String key = normalize(username);
        if (key == null) {
            return null;
        }
        Attempt attempt = attempts.get(key);
        if (attempt == null || attempt.lockUntil == null) {
            return null;
        }
        Instant now = Instant.now();
        if (!attempt.lockUntil.isAfter(now)) {
            attempts.remove(key, attempt);
            return null;
        }
        return Duration.between(now, attempt.lockUntil);
    }

    @Override
    public void onFailure(String username) {
        if (!isEnabled()) {
            return;
        }
        String key = normalize(username);
        if (key == null) {
            return;
        }
        attempts.compute(
                key,
                (ignored, current) -> {
                    Attempt attempt = current == null ? new Attempt() : current;
                    Instant now = Instant.now();
                    if (attempt.lockUntil != null) {
                        if (attempt.lockUntil.isAfter(now)) {
                            return attempt;
                        }
                        attempt.lockUntil = null;
                        attempt.failures = 0;
                    }
                    attempt.failures++;
                    if (attempt.failures >= maxFailures) {
                        attempt.lockUntil = now.plus(lockDuration);
                        attempt.failures = 0;
                    }
                    return attempt;
                });
    }

    @Override
    public void onSuccess(String username) {
        String key = normalize(username);
        if (key != null) {
            attempts.remove(key);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static final class Attempt {
        private int failures;
        private Instant lockUntil;
    }
}

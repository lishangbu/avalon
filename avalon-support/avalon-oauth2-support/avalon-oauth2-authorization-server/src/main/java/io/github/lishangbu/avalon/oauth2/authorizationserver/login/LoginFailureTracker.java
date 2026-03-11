package io.github.lishangbu.avalon.oauth2.authorizationserver.login;

import java.time.Duration;

/**
 * Tracks authentication failures and lock status for a user.
 *
 * <p>Implementations may persist state in-memory or in external storage to support distributed
 * deployments.
 */
public interface LoginFailureTracker {

    /**
     * Returns whether tracking is effectively enabled (e.g. max failures > 0 and lock duration > 0).
     */
    boolean isEnabled();

    /**
     * Returns remaining lock duration for a username, or {@code null} if not locked / disabled.
     */
    Duration getRemainingLock(String username);

    /**
     * Records a failed authentication attempt for the username.
     */
    void onFailure(String username);

    /**
     * Clears tracking for a successful authentication of the username.
     */
    void onSuccess(String username);
}

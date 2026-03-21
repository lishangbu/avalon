package io.github.lishangbu.avalon.oauth2.authorizationserver.login

import java.time.Duration

/**
 * Tracks authentication failures and lock status for a user.
 *
 * <p>Implementations may persist state in-memory or in external storage to support distributed
 * deployments. / Returns whether tracking is effectively enabled (e.g. max failures > 0 and lock
 * duration > 0). Returns remaining lock duration for a username, or {@code null} if not locked /
 * disabled. Records a failed authentication attempt for the username. Clears tracking for a
 * successful authentication of the username.
 */
interface LoginFailureTracker {
    fun isEnabled(): Boolean

    fun getRemainingLock(username: String?): Duration?

    fun onFailure(username: String?)

    fun onSuccess(username: String?)
}

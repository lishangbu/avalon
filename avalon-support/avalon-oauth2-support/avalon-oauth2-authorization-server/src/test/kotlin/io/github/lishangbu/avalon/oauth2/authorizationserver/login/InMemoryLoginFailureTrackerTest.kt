package io.github.lishangbu.avalon.oauth2.authorizationserver.login

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

class InMemoryLoginFailureTrackerTest {
    @Test
    fun disabledWhenPropertiesInvalid() {
        val tracker: LoginFailureTracker = InMemoryLoginFailureTracker(null)

        assertFalse(tracker.isEnabled())
        assertNull(tracker.getRemainingLock("user"))
        tracker.onFailure("user")
        tracker.onSuccess("user")
    }

    @Test
    fun tracksFailuresAndLocksUser() {
        val properties = Oauth2Properties()
        properties.maxLoginFailures = 2
        properties.setLoginLockDuration("50ms")
        val tracker = InMemoryLoginFailureTracker(properties)

        assertTrue(tracker.isEnabled())
        tracker.onFailure("user")
        assertNull(tracker.getRemainingLock("user"))
        tracker.onFailure("user")

        val remaining: Duration? = tracker.getRemainingLock("user")
        assertNotNull(remaining)

        tracker.onFailure("user")
        assertNotNull(tracker.getRemainingLock("user"))

        Thread.sleep(70)
        assertNull(tracker.getRemainingLock("user"))
    }

    @Test
    fun clearsOnSuccessAndIgnoresBlankUsernames() {
        val properties = Oauth2Properties()
        properties.maxLoginFailures = 1
        properties.setLoginLockDuration("20ms")
        val tracker = InMemoryLoginFailureTracker(properties)

        tracker.onFailure(" ")
        assertNull(tracker.getRemainingLock(" "))
        assertNull(tracker.getRemainingLock(null))

        tracker.onFailure("user")
        assertNotNull(tracker.getRemainingLock("user"))
        tracker.onSuccess("user")
        assertNull(tracker.getRemainingLock("user"))
    }

    @Test
    fun disabledWhenDurationNegativeOrZero() {
        val properties = Oauth2Properties()
        properties.maxLoginFailures = 2
        properties.setLoginLockDuration("0s")
        val tracker = InMemoryLoginFailureTracker(properties)

        assertFalse(tracker.isEnabled())
        tracker.onFailure("user")
        assertNull(tracker.getRemainingLock("user"))

        properties.setLoginLockDuration("-1s")
        val negativeTracker = InMemoryLoginFailureTracker(properties)
        assertFalse(negativeTracker.isEnabled())
    }
}

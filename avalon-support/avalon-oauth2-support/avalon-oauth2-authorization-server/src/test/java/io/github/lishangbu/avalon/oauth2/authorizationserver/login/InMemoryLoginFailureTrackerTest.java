package io.github.lishangbu.avalon.oauth2.authorizationserver.login;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class InMemoryLoginFailureTrackerTest {

    @Test
    void disabledWhenPropertiesInvalid() {
        LoginFailureTracker tracker = new InMemoryLoginFailureTracker(null);

        assertFalse(tracker.isEnabled());
        assertNull(tracker.getRemainingLock("user"));
        tracker.onFailure("user");
        tracker.onSuccess("user");
    }

    @Test
    void tracksFailuresAndLocksUser() throws Exception {
        Oauth2Properties properties = new Oauth2Properties();
        properties.setMaxLoginFailures(2);
        properties.setLoginLockDuration("50ms");
        InMemoryLoginFailureTracker tracker = new InMemoryLoginFailureTracker(properties);

        assertTrue(tracker.isEnabled());
        tracker.onFailure("user");
        assertNull(tracker.getRemainingLock("user"));
        tracker.onFailure("user");

        Duration remaining = tracker.getRemainingLock("user");
        assertNotNull(remaining);

        tracker.onFailure("user");
        assertNotNull(tracker.getRemainingLock("user"));

        Thread.sleep(70);
        assertNull(tracker.getRemainingLock("user"));
    }

    @Test
    void clearsOnSuccessAndIgnoresBlankUsernames() {
        Oauth2Properties properties = new Oauth2Properties();
        properties.setMaxLoginFailures(1);
        properties.setLoginLockDuration("20ms");
        InMemoryLoginFailureTracker tracker = new InMemoryLoginFailureTracker(properties);

        tracker.onFailure(" ");
        assertNull(tracker.getRemainingLock(" "));
        assertNull(tracker.getRemainingLock(null));

        tracker.onFailure("user");
        assertNotNull(tracker.getRemainingLock("user"));
        tracker.onSuccess("user");
        assertNull(tracker.getRemainingLock("user"));
    }

    @Test
    void disabledWhenDurationNegativeOrZero() {
        Oauth2Properties properties = new Oauth2Properties();
        properties.setMaxLoginFailures(2);
        properties.setLoginLockDuration("0s");
        InMemoryLoginFailureTracker tracker = new InMemoryLoginFailureTracker(properties);

        assertFalse(tracker.isEnabled());
        tracker.onFailure("user");
        assertNull(tracker.getRemainingLock("user"));

        properties.setLoginLockDuration("-1s");
        InMemoryLoginFailureTracker negativeTracker = new InMemoryLoginFailureTracker(properties);
        assertFalse(negativeTracker.isEnabled());
    }
}

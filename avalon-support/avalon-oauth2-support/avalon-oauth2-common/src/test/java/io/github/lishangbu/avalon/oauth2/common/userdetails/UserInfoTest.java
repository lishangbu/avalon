package io.github.lishangbu.avalon.oauth2.common.userdetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class UserInfoTest {

    @Test
    void constructorCopiesStateAndAttributesAreMutable() {
        UserInfo user = new UserInfo(
                "alice", "pwd", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        user.getAdditionalParameters().put("key", "value");

        assertEquals("alice", user.getUsername());
        assertEquals("alice", user.getName());
        assertEquals("value", user.getAttributes().get("key"));
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    void fullConstructorSetsFlags() {
        UserInfo user = new UserInfo(
                "bob",
                null,
                true,
                false,
                true,
                false,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertEquals("bob", user.getUsername());
        assertTrue(user.isEnabled());
        assertFalse(user.isAccountNonExpired());
        assertTrue(user.isCredentialsNonExpired());
        assertFalse(user.isAccountNonLocked());
    }

    @Test
    void toStringContainsRelevantFields() {
        UserInfo user = new UserInfo(
                "charlie", "pwd", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        user.getAdditionalParameters().put("foo", "bar");

        String text = user.toString();

        assertTrue(text.contains("Username=charlie"));
        assertTrue(text.contains("AdditionalParameters"));
        assertTrue(text.contains("Granted Authorities"));
    }
}

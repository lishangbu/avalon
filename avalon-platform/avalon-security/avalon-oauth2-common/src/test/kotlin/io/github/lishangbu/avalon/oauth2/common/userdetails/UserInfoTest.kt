package io.github.lishangbu.avalon.oauth2.common.userdetails

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority

class UserInfoTest {
    @Test
    fun constructorCopiesStateAndAttributesAreMutable() {
        val user = UserInfo("alice", "pwd", listOf(SimpleGrantedAuthority("ROLE_USER")))

        user.additionalParameters["key"] = "value"

        assertEquals("alice", user.username)
        assertEquals("alice", user.name)
        assertEquals("value", user.attributes["key"])
        assertTrue(user.isAccountNonExpired)
        assertTrue(user.isAccountNonLocked)
        assertTrue(user.isCredentialsNonExpired)
        assertTrue(user.isEnabled)
    }

    @Test
    fun fullConstructorSetsFlags() {
        val user =
            UserInfo(
                "bob",
                null,
                true,
                false,
                true,
                false,
                listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
            )

        assertEquals("bob", user.username)
        assertTrue(user.isEnabled)
        assertFalse(user.isAccountNonExpired)
        assertTrue(user.isCredentialsNonExpired)
        assertFalse(user.isAccountNonLocked)
    }

    @Test
    fun toStringContainsRelevantFields() {
        val user = UserInfo("charlie", "pwd", listOf(SimpleGrantedAuthority("ROLE_USER")))
        user.additionalParameters["foo"] = "bar"

        val text = user.toString()

        assertTrue(text.contains("Username=charlie"))
        assertTrue(text.contains("AdditionalParameters"))
        assertTrue(text.contains("Granted Authorities"))
    }
}

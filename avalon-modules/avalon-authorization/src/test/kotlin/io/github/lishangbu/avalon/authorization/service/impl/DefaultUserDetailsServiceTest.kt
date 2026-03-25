package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.User
import io.github.lishangbu.avalon.authorization.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.core.userdetails.UsernameNotFoundException

class DefaultUserDetailsServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val service = DefaultUserDetailsService(userRepository)

    @Test
    fun returnsUserInfoWithoutAuthoritiesWhenRolesAreMissing() {
        val found = mock(User::class.java)
        `when`(found.username).thenReturn("alice")
        `when`(found.hashedPassword).thenReturn("{noop}pwd")
        `when`(found.roles).thenReturn(emptyList())
        `when`(userRepository.findByAccountWithRoles("alice")).thenReturn(found)

        val details = service.loadUserByUsername("alice")

        assertEquals("alice", details.username)
        assertEquals("{noop}pwd", details.password)
        assertTrue(details.authorities.isEmpty())
    }

    @Test
    fun returnsAuthoritiesFromRoleCodes() {
        val found = user(1L, username = "alice", hashedPassword = "{noop}pwd", roles = listOf(role(10L, "ROLE_ADMIN")))
        `when`(userRepository.findByAccountWithRoles("alice")).thenReturn(found)

        val details = service.loadUserByUsername("alice")

        assertEquals(setOf("ROLE_ADMIN"), details.authorities.map { it.authority }.toSet())
    }

    @Test
    fun throwsWhenUserDoesNotExist() {
        `when`(userRepository.findByAccountWithRoles("missing")).thenReturn(null)

        assertThrows(UsernameNotFoundException::class.java) {
            service.loadUserByUsername("missing")
        }
    }
}

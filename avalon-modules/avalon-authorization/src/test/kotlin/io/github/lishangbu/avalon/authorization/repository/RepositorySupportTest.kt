package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RepositorySupportTest {
    @Test
    fun returnsNullForUnloadedProperty() {
        val user =
            User {
                id = 1L
            }

        assertNull(user.readOrNull { username })
    }

    @Test
    fun rethrowsUnexpectedExceptions() {
        val user =
            User {
                id = 1L
                username = "ash"
            }

        val exception =
            assertThrows(IllegalStateException::class.java) {
                user.readOrNull { throw IllegalStateException("boom") }
            }

        assertEquals("boom", exception.message)
    }
}

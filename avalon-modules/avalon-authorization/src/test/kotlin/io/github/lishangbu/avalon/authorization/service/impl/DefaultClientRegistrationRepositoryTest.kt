package io.github.lishangbu.avalon.authorization.service.impl

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DefaultClientRegistrationRepositoryTest {
    @Test
    fun findByRegistrationIdAlwaysReturnsNull() {
        val repository = DefaultClientRegistrationRepository()

        assertNull(repository.findByRegistrationId("github"))
    }
}

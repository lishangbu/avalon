package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientSpecification
import io.github.lishangbu.avalon.authorization.repository.Oauth2RegisteredClientRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import java.time.Instant

class OauthRegisteredClientServiceImplTest {
    private val repository = mock(Oauth2RegisteredClientRepository::class.java)
    private val service = OauthRegisteredClientServiceImpl(repository)

    @Test
    fun delegatesPageListAndLookupOperations() {
        val pageable = PageRequest.of(0, 10)
        val specification = mock(OauthRegisteredClientSpecification::class.java)
        val page = Page(listOf(registeredClient("client-1")), 1, 1)
        val list = listOf(registeredClient("client-2"))
        val found = registeredClient("client-3")
        `when`(repository.findAll(specification, pageable)).thenReturn(page)
        `when`(repository.findAll(specification)).thenReturn(list)
        `when`(repository.findById("client-3")).thenReturn(found)

        assertSame(page, service.getPageByCondition(specification, pageable))
        assertSame(list, service.listByCondition(specification))
        assertSame(found, service.getById("client-3"))
    }

    @Test
    fun saveGeneratesMissingIdAndIssuedAt() {
        `when`(repository.save(any())).thenAnswer { it.getArgument(0) }

        val saved =
            service.save(
                OauthRegisteredClient {
                    clientId = "public-client"
                    clientSecret = "secret"
                    clientName = "Public Client"
                    scopes = "read,write"
                },
            )

        assertTrue(saved.id.isNotBlank())
        assertEquals("public-client", saved.clientId)
        assertEquals("secret", saved.clientSecret)
        assertEquals("Public Client", saved.clientName)
        assertEquals("read,write", saved.scopes)
        assertNotNull(saved.clientIdIssuedAt)
    }

    @Test
    fun savePreservesProvidedIdAndIssuedAt() {
        val issuedAt = Instant.parse("2026-03-25T00:00:00Z")
        `when`(repository.save(any())).thenAnswer { it.getArgument(0) }

        val saved =
            service.save(
                OauthRegisteredClient {
                    id = "fixed-id"
                    clientId = "trusted-client"
                    clientIdIssuedAt = issuedAt
                },
            )

        assertEquals("fixed-id", saved.id)
        assertEquals(issuedAt, saved.clientIdIssuedAt)
    }

    @Test
    fun updateAndDeleteDelegateToRepository() {
        val registeredClient = registeredClient("client-4")
        `when`(repository.save(any())).thenReturn(registeredClient)

        assertSame(registeredClient, service.update(registeredClient))
        service.removeById("client-4")

        verify(repository).deleteById("client-4")
    }
}

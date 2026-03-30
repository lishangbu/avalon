package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.SaveOauthRegisteredClientInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateOauthRegisteredClientInput
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
        val page = Page(listOf(registeredClientView("client-1")), 1, 1)
        val list = listOf(registeredClientView("client-2"))
        val found = registeredClientView("client-3")
        `when`(repository.pageViews(specification, pageable)).thenReturn(page)
        `when`(repository.listViews(specification)).thenReturn(list)
        `when`(repository.loadViewById("client-3")).thenReturn(found)

        assertSame(page, service.getPageByCondition(specification, pageable))
        assertSame(list, service.listByCondition(specification))
        assertSame(found, service.getById("client-3"))
    }

    @Test
    fun saveGeneratesMissingIdAndIssuedAt() {
        var persisted: OauthRegisteredClient? = null
        `when`(repository.save(anyOauthRegisteredClient())).thenAnswer {
            it.getArgument<OauthRegisteredClient>(0).also { registeredClient -> persisted = registeredClient }
        }
        `when`(repository.loadViewById(any())).thenAnswer {
            registeredClientView(requireNotNull(persisted))
        }

        val saved =
            service.save(
                SaveOauthRegisteredClientInput(
                    id = "",
                    clientId = "public-client",
                    clientSecret = "secret",
                    clientName = "Public Client",
                    clientAuthenticationMethods = "client_secret_basic",
                    authorizationGrantTypes = "client_credentials",
                    scopes = "read,write",
                ),
            )
        val prepared = requireNotNull(persisted)

        assertTrue(prepared.id.isNotBlank())
        assertEquals("public-client", prepared.clientId)
        assertEquals("secret", prepared.clientSecret)
        assertEquals("Public Client", prepared.clientName)
        assertEquals("read,write", prepared.scopes)
        assertNotNull(prepared.clientIdIssuedAt)
        assertEquals(prepared.id, saved.id)
        assertEquals("public-client", saved.clientId)
        assertEquals("secret", saved.clientSecret)
        assertEquals("Public Client", saved.clientName)
        assertEquals("read,write", saved.scopes)
        assertNotNull(saved.clientIdIssuedAt)
        verify(repository).loadViewById(prepared.id)
    }

    @Test
    fun savePreservesProvidedIdAndIssuedAt() {
        val issuedAt = Instant.parse("2026-03-25T00:00:00Z")
        var persisted: OauthRegisteredClient? = null
        `when`(repository.save(anyOauthRegisteredClient())).thenAnswer {
            it.getArgument<OauthRegisteredClient>(0).also { registeredClient -> persisted = registeredClient }
        }
        `when`(repository.loadViewById("fixed-id")).thenAnswer {
            registeredClientView(requireNotNull(persisted))
        }

        val saved =
            service.save(
                SaveOauthRegisteredClientInput(
                    id = "fixed-id",
                    clientId = "trusted-client",
                    clientIdIssuedAt = issuedAt,
                    clientName = "Trusted Client",
                    clientAuthenticationMethods = "private_key_jwt",
                    authorizationGrantTypes = "authorization_code",
                ),
            )
        val prepared = requireNotNull(persisted)

        assertEquals("fixed-id", prepared.id)
        assertEquals(issuedAt, prepared.clientIdIssuedAt)
        assertEquals("fixed-id", saved.id)
        assertEquals(issuedAt, saved.clientIdIssuedAt)
        verify(repository).loadViewById("fixed-id")
    }

    @Test
    fun updateAndDeleteDelegateToRepository() {
        val updatedView = registeredClientView("client-4")
        `when`(repository.save(anyOauthRegisteredClient())).thenReturn(registeredClient("client-4"))
        `when`(repository.loadViewById("client-4")).thenReturn(updatedView)

        assertSame(
            updatedView,
            service.update(
                UpdateOauthRegisteredClientInput(
                    id = "client-4",
                    clientId = "client-4",
                    clientName = "Client client-4",
                    clientAuthenticationMethods = "client_secret_basic",
                    authorizationGrantTypes = "client_credentials",
                    scopes = "read",
                ),
            ),
        )
        service.removeById("client-4")

        verify(repository).loadViewById("client-4")
        verify(repository).deleteById("client-4")
    }
}

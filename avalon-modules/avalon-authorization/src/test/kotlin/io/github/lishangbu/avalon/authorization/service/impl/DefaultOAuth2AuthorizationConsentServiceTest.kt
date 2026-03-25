package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsent
import io.github.lishangbu.avalon.authorization.repository.OauthAuthorizationConsentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository

class DefaultOAuth2AuthorizationConsentServiceTest {
    private val consentRepository = mock(OauthAuthorizationConsentRepository::class.java)
    private val registeredClientRepository = mock(RegisteredClientRepository::class.java)
    private val service =
        DefaultOAuth2AuthorizationConsentService(
            consentRepository,
            registeredClientRepository,
        )

    @Test
    fun saveMapsAuthoritiesIntoEntity() {
        var persisted: OauthAuthorizationConsent? = null
        Mockito
            .doAnswer {
                persisted = it.getArgument(0)
                persisted
            }.`when`(consentRepository)
            .save(any())

        service.save(
            OAuth2AuthorizationConsent
                .withId("client-id", "alice")
                .authority(SimpleGrantedAuthority("read"))
                .authority(SimpleGrantedAuthority("write"))
                .build(),
        )

        val saved = requireNotNull(persisted)
        assertEquals("client-id", saved.id.registeredClientId)
        assertEquals("alice", saved.id.principalName)
        assertEquals(setOf("read", "write"), saved.authorities!!.split(',').toSet())
    }

    @Test
    fun removeDelegatesToRepository() {
        val consent =
            OAuth2AuthorizationConsent
                .withId("client-id", "alice")
                .authority(SimpleGrantedAuthority("read"))
                .build()

        service.remove(consent)

        verify(consentRepository).deleteByRegisteredClientIdAndPrincipalName("client-id", "alice")
    }

    @Test
    fun findByIdRejectsBlankArguments() {
        assertEquals(
            "registeredClientId cannot be empty",
            assertThrows(IllegalArgumentException::class.java) {
                service.findById(" ", "alice")
            }.message,
        )
        assertEquals(
            "principalName cannot be empty",
            assertThrows(IllegalArgumentException::class.java) {
                service.findById("client-id", " ")
            }.message,
        )
    }

    @Test
    fun findByIdReturnsNullWhenConsentIsMissing() {
        `when`(consentRepository.findByRegisteredClientIdAndPrincipalName("client-id", "alice")).thenReturn(null)

        assertNull(service.findById("client-id", "alice"))
        verifyNoInteractions(registeredClientRepository)
    }

    @Test
    fun findByIdThrowsWhenRegisteredClientCannotBeLoaded() {
        `when`(consentRepository.findByRegisteredClientIdAndPrincipalName("client-id", "alice")).thenReturn(
            OauthAuthorizationConsent {
                id {
                    registeredClientId = "client-id"
                    principalName = "alice"
                }
                authorities = "read,write"
            },
        )
        `when`(registeredClientRepository.findById("client-id")).thenReturn(null)

        assertThrows(DataRetrievalFailureException::class.java) {
            service.findById("client-id", "alice")
        }
    }

    @Test
    fun findByIdRejectsConsentWithoutRegisteredClientId() {
        val consentId = mock(io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsentId::class.java)
        val consent = mock(OauthAuthorizationConsent::class.java)
        `when`(consent.id).thenReturn(consentId)
        `when`(consentRepository.findByRegisteredClientIdAndPrincipalName("client-id", "alice")).thenReturn(consent)

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                service.findById("client-id", "alice")
            }

        assertEquals("registeredClientId cannot be null", exception.message)
    }

    @Test
    fun findByIdRejectsConsentWithoutPrincipalName() {
        val consentId = mock(io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsentId::class.java)
        val consent = mock(OauthAuthorizationConsent::class.java)
        `when`(consent.id).thenReturn(consentId)
        `when`(consentId.registeredClientId).thenReturn("client-id")
        `when`(consentRepository.findByRegisteredClientIdAndPrincipalName("client-id", "alice")).thenReturn(consent)

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                service.findById("client-id", "alice")
            }

        assertEquals("principalName cannot be null", exception.message)
    }

    @Test
    fun findByIdMapsAuthoritiesBackToConsent() {
        `when`(consentRepository.findByRegisteredClientIdAndPrincipalName("client-id", "alice")).thenReturn(
            OauthAuthorizationConsent {
                id {
                    registeredClientId = "client-id"
                    principalName = "alice"
                }
                authorities = "read,write"
            },
        )
        `when`(registeredClientRepository.findById("client-id")).thenReturn(registeredClient())

        val consent = service.findById("client-id", "alice")

        assertNotNull(consent)
        assertEquals("client-id", consent!!.registeredClientId)
        assertEquals("alice", consent.principalName)
        assertEquals(setOf("read", "write"), consent.authorities.map { it.authority }.toSet())
    }
}

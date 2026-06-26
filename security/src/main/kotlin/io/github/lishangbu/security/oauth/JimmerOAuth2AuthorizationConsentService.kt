package io.github.lishangbu.security.oauth

import io.github.lishangbu.security.entity.OAuth2AuthorizationConsentRecord
import io.github.lishangbu.security.entity.OAuth2AuthorizationConsentRecordId
import io.github.lishangbu.security.repository.OAuth2AuthorizationConsentRecordRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService

/**
 * 基于 Jimmer Repository 的 SAS 授权同意服务。
 */
class JimmerOAuth2AuthorizationConsentService(
	private val consentRepository: OAuth2AuthorizationConsentRecordRepository,
) : OAuth2AuthorizationConsentService {
	override fun save(authorizationConsent: OAuth2AuthorizationConsent) {
		consentRepository.save(
			OAuth2AuthorizationConsentRecord {
				id = OAuth2AuthorizationConsentRecordId {
					registeredClientId = authorizationConsent.registeredClientId
					principalName = authorizationConsent.principalName
				}
				authorities = authorizationConsent.authorities
					.mapNotNull { it.authority }
					.sorted()
					.joinToString(AUTHORITY_DELIMITER)
			},
		)
	}

	override fun remove(authorizationConsent: OAuth2AuthorizationConsent) {
		consentRepository.deleteById(
			OAuth2AuthorizationConsentRecordId {
				registeredClientId = authorizationConsent.registeredClientId
				principalName = authorizationConsent.principalName
			},
		)
	}

	override fun findById(registeredClientId: String, principalName: String): OAuth2AuthorizationConsent? =
		consentRepository.findNullable(
			OAuth2AuthorizationConsentRecordId {
				this.registeredClientId = registeredClientId
				this.principalName = principalName
			},
		)?.toAuthorizationConsent()

	private fun OAuth2AuthorizationConsentRecord.toAuthorizationConsent(): OAuth2AuthorizationConsent =
		OAuth2AuthorizationConsent.withId(id.registeredClientId, id.principalName)
			.authorities { grantedAuthorities ->
				grantedAuthorities.addAll(authorities.splitAuthorities().map(::SimpleGrantedAuthority))
			}
			.build()

	private fun String.splitAuthorities(): List<String> =
		split(AUTHORITY_DELIMITER)
			.map(String::trim)
			.filter(String::isNotEmpty)

	private companion object {
		private const val AUTHORITY_DELIMITER = " "
	}
}

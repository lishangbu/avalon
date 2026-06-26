package io.github.lishangbu.security.repository

import io.github.lishangbu.security.entity.OAuth2AuthorizationConsentRecord
import io.github.lishangbu.security.entity.OAuth2AuthorizationConsentRecordId
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * OAuth2 授权同意记录的 Jimmer Repository。
 */
interface OAuth2AuthorizationConsentRecordRepository :
	KRepository<OAuth2AuthorizationConsentRecord, OAuth2AuthorizationConsentRecordId>

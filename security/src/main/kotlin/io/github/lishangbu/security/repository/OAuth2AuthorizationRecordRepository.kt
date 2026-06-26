package io.github.lishangbu.security.repository

import io.github.lishangbu.security.entity.OAuth2AuthorizationRecord
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * OAuth2 授权记录的 Jimmer Repository。
 */
interface OAuth2AuthorizationRecordRepository : KRepository<OAuth2AuthorizationRecord, String>

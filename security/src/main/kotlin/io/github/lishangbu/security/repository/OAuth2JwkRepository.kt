package io.github.lishangbu.security.repository

import io.github.lishangbu.security.entity.OAuth2Jwk
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * OAuth2 JWK 的 Jimmer Repository。
 */
interface OAuth2JwkRepository : KRepository<OAuth2Jwk, Long>

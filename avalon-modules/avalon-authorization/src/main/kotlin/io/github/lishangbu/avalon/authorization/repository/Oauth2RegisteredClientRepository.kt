package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable

/**
 * OauthRegisteredClient 数据访问 Mapper
 *
 * 提供对 OauthRegisteredClient 实体的查询方法
 *
 * @author lishangbu
 * @since 2023-10-08
 */
interface Oauth2RegisteredClientRepository {
    fun findAll(example: Example<OauthRegisteredClient>?): List<OauthRegisteredClient>

    fun findAll(
        example: Example<OauthRegisteredClient>?,
        pageable: Pageable,
    ): Page<OauthRegisteredClient>

    fun findById(id: String): OauthRegisteredClient?

    fun save(registeredClient: OauthRegisteredClient): OauthRegisteredClient

    fun saveAndFlush(registeredClient: OauthRegisteredClient): OauthRegisteredClient

    fun deleteById(id: String)

    fun flush()

    fun findByClientId(clientId: String): OauthRegisteredClient?
}

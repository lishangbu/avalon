package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable

/**
 * OAuth2 注册客户端仓储接口
 *
 * 定义OAuth2 注册客户端数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2023-10-08
 */
interface Oauth2RegisteredClientRepository {
    /** 按条件查询 OAuth2 注册客户端列表 */
    fun findAll(example: Example<OauthRegisteredClient>?): List<OauthRegisteredClient>

    /** 按条件分页查询 OAuth2 注册客户端 */
    fun findAll(
        example: Example<OauthRegisteredClient>?,
        pageable: Pageable,
    ): Page<OauthRegisteredClient>

    /** 按 ID 查询 OAuth2 注册客户端 */
    fun findById(id: String): OauthRegisteredClient?

    /** 保存OAuth2 注册客户端 */
    fun save(registeredClient: OauthRegisteredClient): OauthRegisteredClient

    /** 保存OAuth2 注册客户端并立即刷新 */
    fun saveAndFlush(registeredClient: OauthRegisteredClient): OauthRegisteredClient

    /** 按 ID 删除 OAuth2 注册客户端 */
    fun deleteById(id: String)

    /** 刷新持久化上下文 */
    fun flush()

    /** 根据客户端 ID查找OAuth2 注册客户端 */
    fun findByClientId(clientId: String): OauthRegisteredClient?
}

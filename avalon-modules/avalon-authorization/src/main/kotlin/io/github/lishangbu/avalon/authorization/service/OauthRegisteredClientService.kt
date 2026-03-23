package io.github.lishangbu.avalon.authorization.service

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/**
 * OAuth2 注册客户端服务
 *
 * 提供 OAuth2 注册客户端的增删改查能力
 *
 * @author lishangbu
 * @since 2026/3/19
 */
interface OauthRegisteredClientService {
    /** 根据条件分页查询注册客户端。 */
    fun getPageByCondition(
        registeredClient: OauthRegisteredClient,
        pageable: Pageable,
    ): Page<OauthRegisteredClient>

    /** 根据条件查询注册客户端列表。 */
    fun listByCondition(registeredClient: OauthRegisteredClient): List<OauthRegisteredClient>

    /** 根据 ID 查询注册客户端。 */
    fun getById(id: String): OauthRegisteredClient?

    /** 新增注册客户端。 */
    fun save(registeredClient: OauthRegisteredClient): OauthRegisteredClient

    /** 更新注册客户端。 */
    fun update(registeredClient: OauthRegisteredClient): OauthRegisteredClient

    /** 根据 ID 删除注册客户端。 */
    fun removeById(id: String)
}

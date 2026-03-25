package io.github.lishangbu.avalon.authorization.service

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientSpecification
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/**
 * OAuth2 注册客户端服务
 *
 * 定义 OAuth2 注册客户端的管理能力
 *
 * @author lishangbu
 * @since 2026/3/19
 */
interface OauthRegisteredClientService {
    /** 按条件分页查询 OAuth2 注册客户端 */
    fun getPageByCondition(
        specification: OauthRegisteredClientSpecification,
        pageable: Pageable,
    ): Page<OauthRegisteredClient>

    /** 按条件查询 OAuth2 注册客户端列表 */
    fun listByCondition(specification: OauthRegisteredClientSpecification): List<OauthRegisteredClient>

    /** 按 ID 查询 OAuth2 注册客户端 */
    fun getById(id: String): OauthRegisteredClient?

    /** 保存 OAuth2 注册客户端 */
    fun save(registeredClient: OauthRegisteredClient): OauthRegisteredClient

    /** 更新 OAuth2 注册客户端 */
    fun update(registeredClient: OauthRegisteredClient): OauthRegisteredClient

    /** 按 ID 删除 OAuth2 注册客户端 */
    fun removeById(id: String)
}

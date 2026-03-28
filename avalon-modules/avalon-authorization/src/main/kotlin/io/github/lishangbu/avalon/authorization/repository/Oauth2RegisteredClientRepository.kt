package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientSpecification
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.data.domain.Pageable

/**
 * OAuth2 注册客户端仓储接口
 *
 * 定义OAuth2 注册客户端数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2023-10-08
 */
interface Oauth2RegisteredClientRepository :
    KRepository<OauthRegisteredClient, String>,
    Oauth2RegisteredClientRepositoryExt

interface Oauth2RegisteredClientRepositoryExt {
    /** 按条件查询 OAuth2 注册客户端列表 */
    fun findAll(specification: OauthRegisteredClientSpecification?): List<OauthRegisteredClient>

    /** 按条件分页查询 OAuth2 注册客户端 */
    fun findAll(
        specification: OauthRegisteredClientSpecification?,
        pageable: Pageable,
    ): Page<OauthRegisteredClient>

    /** 按 ID 删除 OAuth2 注册客户端 */
    fun removeById(id: String)

    /** 根据客户端 ID查找OAuth2 注册客户端 */
    fun findByClientId(clientId: String): OauthRegisteredClient?
}

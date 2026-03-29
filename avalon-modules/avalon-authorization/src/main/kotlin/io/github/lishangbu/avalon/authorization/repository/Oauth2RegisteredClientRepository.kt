package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.Specification
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
interface Oauth2RegisteredClientRepository : KRepository<OauthRegisteredClient, String> {
    /** 按条件查询 OAuth2 注册客户端列表 */
    fun findAll(specification: Specification<OauthRegisteredClient>?): List<OauthRegisteredClient> =
        sql
            .createQuery(OauthRegisteredClient::class) {
                specification?.let(::where)
                select(table)
            }.execute()

    /** 按条件分页查询 OAuth2 注册客户端 */
    fun findAll(
        specification: Specification<OauthRegisteredClient>?,
        pageable: Pageable,
    ): Page<OauthRegisteredClient> =
        sql
            .createQuery(OauthRegisteredClient::class) {
                specification?.let(::where)
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 根据客户端 ID查找OAuth2 注册客户端 */
    fun findByClientId(clientId: String): OauthRegisteredClient?
}

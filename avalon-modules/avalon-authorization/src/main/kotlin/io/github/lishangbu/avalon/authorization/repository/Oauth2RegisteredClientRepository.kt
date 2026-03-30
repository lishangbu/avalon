package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
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

    /** 按条件查询 OAuth2 注册客户端视图列表 */
    fun listViews(specification: Specification<OauthRegisteredClient>?): List<OauthRegisteredClientView> =
        sql
            .createQuery(OauthRegisteredClient::class) {
                specification?.let(::where)
                select(table.fetch(OauthRegisteredClientView::class))
            }.execute()

    /** 按条件分页查询 OAuth2 注册客户端视图 */
    fun pageViews(
        specification: Specification<OauthRegisteredClient>?,
        pageable: Pageable,
    ): Page<OauthRegisteredClientView> =
        sql
            .createQuery(OauthRegisteredClient::class) {
                specification?.let(::where)
                select(table.fetch(OauthRegisteredClientView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询 OAuth2 注册客户端视图 */
    fun loadViewById(id: String): OauthRegisteredClientView? =
        sql
            .createQuery(OauthRegisteredClient::class) {
                where(table.id eq id)
                select(table.fetch(OauthRegisteredClientView::class))
            }.execute()
            .firstOrNull()

    /** 根据客户端 ID查找OAuth2 注册客户端 */
    fun findByClientId(clientId: String): OauthRegisteredClient?
}

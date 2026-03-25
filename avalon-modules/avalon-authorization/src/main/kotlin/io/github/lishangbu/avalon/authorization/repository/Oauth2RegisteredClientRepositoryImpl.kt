package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.entity.clientId
import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientSpecification
import io.github.lishangbu.avalon.authorization.entity.id
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class Oauth2RegisteredClientRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : Oauth2RegisteredClientRepository {
    /** 按条件查询 OAuth2 注册客户端列表 */
    override fun findAll(specification: OauthRegisteredClientSpecification?): List<OauthRegisteredClient> =
        sql
            .createQuery(OauthRegisteredClient::class) {
                specification?.let { where(it) }
                select(table)
            }.execute()

    /** 按条件分页查询 OAuth2 注册客户端 */
    override fun findAll(
        specification: OauthRegisteredClientSpecification?,
        pageable: Pageable,
    ): Page<OauthRegisteredClient> =
        sql
            .createQuery(OauthRegisteredClient::class) {
                specification?.let { where(it) }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询 OAuth2 注册客户端 */
    override fun findById(id: String): OauthRegisteredClient? = sql.findById(OauthRegisteredClient::class, id)

    /** 保存 OAuth2 注册客户端 */
    override fun save(registeredClient: OauthRegisteredClient): OauthRegisteredClient = sql.save(registeredClient).modifiedEntity

    /** 保存 OAuth2 注册客户端并立即刷新 */
    override fun saveAndFlush(registeredClient: OauthRegisteredClient): OauthRegisteredClient = save(registeredClient)

    /** 按 ID 删除 OAuth2 注册客户端 */
    override fun deleteById(id: String) {
        sql
            .createDelete(OauthRegisteredClient::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** Jimmer 无需显式刷新，保留空实现 */
    override fun flush() = Unit

    /** 按客户端 ID 查询 OAuth2 注册客户端 */
    override fun findByClientId(clientId: String): OauthRegisteredClient? =
        sql
            .createQuery(OauthRegisteredClient::class) {
                where(table.clientId eq clientId)
                select(table)
            }.execute()
            .firstOrNull()
}

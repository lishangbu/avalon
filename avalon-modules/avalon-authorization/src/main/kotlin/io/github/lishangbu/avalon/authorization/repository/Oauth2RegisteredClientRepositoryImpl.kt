package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.entity.clientId
import io.github.lishangbu.avalon.authorization.entity.clientName
import io.github.lishangbu.avalon.authorization.entity.id
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class Oauth2RegisteredClientRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : Oauth2RegisteredClientRepository {
    /** 按条件查询 OAuth2 注册客户端列表 */
    override fun findAll(example: Example<OauthRegisteredClient>?): List<OauthRegisteredClient> {
        val probe = example?.probe
        return sql
            .createQuery(OauthRegisteredClient::class) {
                probe.readId().takeFilter()?.let { where(table.id eq it) }
                probe.readClientId().takeFilter()?.let { where(table.clientId ilike "%$it%") }
                probe.readClientName().takeFilter()?.let { where(table.clientName ilike "%$it%") }
                select(table)
            }.execute()
    }

    /** 按条件分页查询 OAuth2 注册客户端 */
    override fun findAll(
        example: Example<OauthRegisteredClient>?,
        pageable: Pageable,
    ): Page<OauthRegisteredClient> {
        val probe = example?.probe
        return sql
            .createQuery(OauthRegisteredClient::class) {
                probe.readId().takeFilter()?.let { where(table.id eq it) }
                probe.readClientId().takeFilter()?.let { where(table.clientId ilike "%$it%") }
                probe.readClientName().takeFilter()?.let { where(table.clientName ilike "%$it%") }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)
    }

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

    /** 安全读取主键 */
    private fun OauthRegisteredClient?.readId(): String? = readOrNull { id }

    /** 安全读取客户端 ID */
    private fun OauthRegisteredClient?.readClientId(): String? = readOrNull { clientId }

    /** 安全读取客户端名称 */
    private fun OauthRegisteredClient?.readClientName(): String? = readOrNull { clientName }
}

package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class Oauth2RegisteredClientRepositoryImpl(
    private val sql: KSqlClient,
) : Oauth2RegisteredClientRepository {
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

    override fun findById(id: String): OauthRegisteredClient? = sql.findById(OauthRegisteredClient::class, id)

    override fun save(registeredClient: OauthRegisteredClient): OauthRegisteredClient = sql.save(registeredClient).modifiedEntity

    override fun saveAndFlush(registeredClient: OauthRegisteredClient): OauthRegisteredClient = save(registeredClient)

    override fun deleteById(id: String) {
        sql
            .createDelete(OauthRegisteredClient::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    override fun flush() = Unit

    override fun findByClientId(clientId: String): OauthRegisteredClient? =
        sql
            .createQuery(OauthRegisteredClient::class) {
                where(table.clientId eq clientId)
                select(table)
            }.execute()
            .firstOrNull()

    private fun OauthRegisteredClient?.readId(): String? = readOrNull { id }

    private fun OauthRegisteredClient?.readClientId(): String? = readOrNull { clientId }

    private fun OauthRegisteredClient?.readClientName(): String? = readOrNull { clientName }
}

package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.AuthenticationLog
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository

@Repository
class AuthenticationLogRepositoryImpl(
    private val sql: KSqlClient,
) : AuthenticationLogRepository {
    override fun save(authenticationLog: AuthenticationLog): AuthenticationLog =
        sql
            .save(authenticationLog) {
                val mode = authenticationLog.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    override fun saveAndFlush(authenticationLog: AuthenticationLog): AuthenticationLog = save(authenticationLog)
}

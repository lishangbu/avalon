package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.AuthenticationLog
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository

@Repository
class AuthenticationLogRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : AuthenticationLogRepository {
    /** 保存认证日志 */
    override fun save(authenticationLog: AuthenticationLog): AuthenticationLog =
        sql
            .save(authenticationLog) {
                val mode = authenticationLog.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    /** 保存认证日志并立即刷新 */
    override fun saveAndFlush(authenticationLog: AuthenticationLog): AuthenticationLog = save(authenticationLog)
}

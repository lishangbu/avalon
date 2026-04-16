package io.github.lishangbu.avalon.shared.infra.sql

import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.SqlConnection

/**
 * 用协程风格在 Vert.x SQL `Pool` 上执行本地事务。
 *
 * 这里统一收口连接获取、事务提交/回滚与连接释放，
 * 让各上下文只关注自己的 SQL 读写逻辑，不再重复样板事务代码。
 *
 * @param transactionBody 在同一个 `SqlConnection` 上执行的事务体。
 * @return 事务成功提交后的结果。
 */
suspend fun <T> Pool.withSuspendingTransaction(transactionBody: suspend (SqlConnection) -> T): T {
    val connection = connection.awaitSuspending()
    try {
        val transaction = connection.begin().awaitSuspending()
        return try {
            val result = transactionBody(connection)
            transaction.commit().awaitSuspending()
            result
        } catch (exception: Throwable) {
            transaction.rollback().awaitSuspending()
            throw exception
        }
    } finally {
        connection.close().awaitSuspending()
    }
}
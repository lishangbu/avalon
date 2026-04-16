package io.github.lishangbu.avalon.shared.infra.outbox

import io.vertx.mutiny.sqlclient.SqlConnection

/**
 * Outbox 写入器契约。
 *
 * 调用方必须在同一个本地事务里先写业务数据，再调用该接口把需要传播的事实追加到 outbox。
 * 这样才能保证“业务提交成功”和“消息可被后续 dispatcher 发现”来自同一次提交。
 */
interface OutboxMessageWriter {
    /**
     * 把一条 outbox 消息追加到当前事务中。
     *
     * @param connection 当前本地事务对应的 SQL 连接。
     * @param draft 待写入的 outbox 消息草稿。
     * @return 已持久化的 outbox 消息快照。
     */
    suspend fun append(
        connection: SqlConnection,
        draft: OutboxMessageDraft,
    ): StoredOutboxMessage
}
package io.github.lishangbu.avalon.shared.infra.outbox

import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.github.lishangbu.avalon.shared.infra.sql.addInstant
import io.github.lishangbu.avalon.shared.infra.sql.first
import io.vertx.mutiny.sqlclient.SqlConnection
import io.vertx.mutiny.sqlclient.Tuple
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 基于 PostgreSQL 的 outbox 写入器。
 *
 * 该实现只负责把消息追加到 `integration.outbox_event`，不负责决定
 * 哪些业务事实应该被写入 outbox；调用方仍需先完成本地业务写入，再调用此写入器。
 */
@ApplicationScoped
class SqlOutboxMessageWriter : OutboxMessageWriter {
    override suspend fun append(
        connection: SqlConnection,
        draft: OutboxMessageDraft,
    ): StoredOutboxMessage =
        connection.preparedQuery(INSERT_OUTBOX_MESSAGE_SQL)
            .execute(
                Tuple.tuple()
                    .addString(draft.eventId.toString())
                    .addString(draft.ownerContext)
                    .addString(draft.aggregateType)
                    .addString(draft.aggregateId)
                    .addString(draft.eventType)
                    .addString(draft.payload)
                    .addString(draft.headers)
                    .addInstant(draft.availableAt)
                    .addInstant(draft.occurredAt)
                    .addValue(draft.traceId),
            ).awaitSuspending()
            .first()
            .let(::mapStoredOutboxMessage)

    private companion object {
        private val INSERT_OUTBOX_MESSAGE_SQL =
            """
            INSERT INTO integration.outbox_event (
                event_id,
                owner_context,
                aggregate_type,
                aggregate_id,
                event_type,
                payload,
                headers,
                status,
                retry_count,
                available_at,
                occurred_at,
                trace_id
            )
            VALUES ($1::uuid, $2, $3, $4, $5, $6::jsonb, $7::jsonb, 'PENDING', 0, $8::timestamptz, $9::timestamptz, $10)
            RETURNING id, event_id::text AS event_id, owner_context, aggregate_type, aggregate_id, event_type, payload, headers, status, retry_count, available_at, occurred_at, claim_token::text AS claim_token, claimed_at, trace_id
            """.trimIndent()
    }
}
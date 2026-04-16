package io.github.lishangbu.avalon.shared.infra.outbox

import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.github.lishangbu.avalon.shared.infra.sql.addInstant
import io.github.lishangbu.avalon.shared.infra.sql.toRows
import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.Tuple
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.util.*
import java.util.UUID

/**
 * 基于 PostgreSQL 的 outbox 消息状态仓库。
 *
 * 该实现负责认领待发布消息、标记成功和标记失败。认领时会把消息状态切到
 * `DISPATCHING`，并写入批次级别的认领令牌，避免后续更新误伤到已被重新认领的记录。
 */
@ApplicationScoped
class SqlOutboxMessageStore(
    private val pool: Pool,
) : OutboxMessageStore {
    override suspend fun claimReadyMessages(
        limit: Int,
        referenceTime: Instant,
    ): List<StoredOutboxMessage> {
        if (limit <= 0) {
            return emptyList()
        }

        val claimToken = UUID.randomUUID()
        val staleReferenceTime = referenceTime.minus(OUTBOX_CLAIM_LEASE)
        return pool.preparedQuery(CLAIM_READY_MESSAGES_SQL)
            .execute(
                Tuple.tuple()
                    .addInstant(referenceTime)
                    .addInstant(staleReferenceTime)
                    .addInteger(limit)
                    .addString(claimToken.toString()),
            ).awaitSuspending()
            .toRows()
            .map(::mapStoredOutboxMessage)
            .sortedWith(compareBy<StoredOutboxMessage>({ it.availableAt }, { it.id }))
    }

    override suspend fun markPublished(
        messageId: UUID,
        claimToken: UUID,
        publishedAt: Instant,
    ) {
        val updatedCount =
            pool.preparedQuery(MARK_PUBLISHED_SQL)
                .execute(
                    Tuple.tuple()
                        .addValue(messageId)
                        .addString(claimToken.toString())
                        .addInstant(publishedAt),
                ).awaitSuspending()
                .rowCount()

        if (updatedCount == 0) {
            throw IllegalStateException("Outbox message $messageId could not be marked as published because the claim token no longer matched.")
        }
    }

    override suspend fun markFailed(
        messageId: UUID,
        claimToken: UUID,
        nextAvailableAt: Instant,
        lastError: String,
    ) {
        val updatedCount =
            pool.preparedQuery(MARK_FAILED_SQL)
                .execute(
                    Tuple.tuple()
                        .addValue(messageId)
                        .addString(claimToken.toString())
                        .addInstant(nextAvailableAt)
                        .addString(lastError),
                ).awaitSuspending()
                .rowCount()

        if (updatedCount == 0) {
            throw IllegalStateException("Outbox message $messageId could not be marked as failed because the claim token no longer matched.")
        }
    }

    private companion object {
        private val CLAIM_READY_MESSAGES_SQL =
            """
            WITH candidate AS (
                SELECT id, available_at
                FROM integration.outbox_event
                WHERE (
                    status IN ('PENDING', 'FAILED')
                    AND available_at <= $1::timestamptz
                )
                OR (
                    status = 'DISPATCHING'
                    AND claimed_at <= $2::timestamptz
                )
                ORDER BY available_at, id
                LIMIT $3
                FOR UPDATE SKIP LOCKED
            )
            UPDATE integration.outbox_event oe
            SET status = 'DISPATCHING',
                claim_token = $4::uuid,
                claimed_at = $1::timestamptz
            FROM candidate
            WHERE oe.id = candidate.id
            RETURNING oe.id, oe.event_id::text AS event_id, oe.owner_context, oe.aggregate_type, oe.aggregate_id, oe.event_type, oe.payload, oe.headers, oe.status, oe.retry_count, oe.available_at, oe.occurred_at, oe.claim_token::text AS claim_token, oe.claimed_at, oe.trace_id
            """.trimIndent()

        private val MARK_PUBLISHED_SQL =
            """
            UPDATE integration.outbox_event
            SET status = 'PUBLISHED',
                published_at = $3::timestamptz,
                last_error = NULL,
                claim_token = NULL,
                claimed_at = NULL
            WHERE id = $1
              AND claim_token = $2::uuid
            """.trimIndent()

        private val MARK_FAILED_SQL =
            """
            UPDATE integration.outbox_event
            SET status = 'FAILED',
                retry_count = retry_count + 1,
                available_at = $3::timestamptz,
                published_at = NULL,
                last_error = $4,
                claim_token = NULL,
                claimed_at = NULL
            WHERE id = $1
              AND claim_token = $2::uuid
            """.trimIndent()
    }
}


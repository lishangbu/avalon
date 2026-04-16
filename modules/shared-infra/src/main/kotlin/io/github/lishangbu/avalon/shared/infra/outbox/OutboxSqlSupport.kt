package io.github.lishangbu.avalon.shared.infra.outbox

import io.github.lishangbu.avalon.shared.infra.sql.instant
import io.vertx.mutiny.sqlclient.Row
import java.time.Duration
import java.util.*
import java.util.UUID

internal val OUTBOX_CLAIM_LEASE: Duration = Duration.ofMinutes(2)

internal fun mapStoredOutboxMessage(row: Row): StoredOutboxMessage =
    StoredOutboxMessage(
        id = row.getUUID("id"),
        eventId = UUID.fromString(row.getString("event_id")),
        ownerContext = row.getString("owner_context"),
        aggregateType = row.getString("aggregate_type"),
        aggregateId = row.getString("aggregate_id"),
        eventType = row.getString("event_type"),
        payload = row.getString("payload"),
        headers = row.getString("headers"),
        status = OutboxStatus.valueOf(row.getString("status")),
        retryCount = row.getInteger("retry_count"),
        availableAt = row.instant("available_at"),
        occurredAt = row.instant("occurred_at"),
        claimToken = row.getString("claim_token")?.let(UUID::fromString),
        claimedAt = row.getOffsetDateTime("claimed_at")?.toInstant(),
        traceId = row.getString("trace_id"),
    )


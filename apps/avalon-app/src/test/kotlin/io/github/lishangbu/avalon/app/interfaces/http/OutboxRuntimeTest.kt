package io.github.lishangbu.avalon.app.interfaces.http

import io.github.lishangbu.avalon.shared.application.time.ClockProvider
import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.github.lishangbu.avalon.shared.infra.outbox.*
import io.quarkus.test.junit.QuarkusTest
import io.vertx.mutiny.sqlclient.*
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Alternative
import jakarta.inject.Inject
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
class OutboxRuntimeTest {
    @Inject
    lateinit var pool: Pool

    @Inject
    lateinit var writer: OutboxMessageWriter

    @Inject
    lateinit var dispatchExecutor: OutboxDispatchExecutor

    @Inject
    lateinit var clockProvider: ClockProvider

    @Inject
    lateinit var recordingPublisher: RecordingOutboxMessagePublisher

    @Test
    fun shouldWriteAndDispatchOutboxMessage() {
        runBlocking {
            recordingPublisher.clear()

            val eventId = UUID.randomUUID()
            val referenceTime = clockProvider.currentInstant()
            var messageId: UUID? = null

            try {
                messageId =
                    withTransaction<UUID>(pool) { connection ->
                        writer.append(
                            connection,
                            OutboxMessageDraft(
                                eventId = eventId,
                                ownerContext = "catalog",
                                aggregateType = "species",
                                aggregateId = "species-9001",
                                eventType = "SpeciesCreated",
                                payload = """{"speciesId":9001,"code":"FLAMELING"}""",
                                headers = """{"source":"outbox-test"}""",
                                occurredAt = referenceTime,
                                availableAt = referenceTime,
                                traceId = "trace-1",
                            ),
                        ).id
                    }
                val storedMessageId = requireNotNull(messageId)

                val writtenMessage =
                    pool.preparedQuery(
                        """
                        SELECT status, claim_token, claimed_at
                        FROM integration.outbox_event
                        WHERE id = $1
                        """.trimIndent(),
                    ).execute(Tuple.of(storedMessageId))
                        .awaitSuspending()
                        .first()

                assertEquals("PENDING", writtenMessage.getString("status"))
                assertNull(writtenMessage.getString("claim_token"))
                assertNull(writtenMessage.getOffsetDateTime("claimed_at"))

                dispatchExecutor.dispatchReadyMessages(referenceTime.plusSeconds(1))

                assertEquals(1, recordingPublisher.publishedMessages.size)
                val publishedMessage = recordingPublisher.publishedMessages.single()
                assertEquals(messageId, publishedMessage.id)
                assertEquals(eventId, publishedMessage.eventId)
                assertEquals(OutboxStatus.DISPATCHING, publishedMessage.status)
                assertNotNull(publishedMessage.claimToken)
                assertNotNull(publishedMessage.claimedAt)

                val dispatchedMessage =
                    pool.preparedQuery(
                        """
                        SELECT status, retry_count, claim_token, claimed_at, published_at, last_error
                        FROM integration.outbox_event
                        WHERE id = $1
                        """.trimIndent(),
                    ).execute(Tuple.of(storedMessageId))
                        .awaitSuspending()
                        .first()

                assertEquals("PUBLISHED", dispatchedMessage.getString("status"))
                assertEquals(0, dispatchedMessage.getInteger("retry_count"))
                assertNull(dispatchedMessage.getString("claim_token"))
                assertNull(dispatchedMessage.getOffsetDateTime("claimed_at"))
                assertNotNull(dispatchedMessage.getOffsetDateTime("published_at"))
                assertNull(dispatchedMessage.getString("last_error"))
            } finally {
                messageId?.let {
                    pool.preparedQuery("DELETE FROM integration.outbox_event WHERE id = $1")
                        .execute(Tuple.of(it))
                        .awaitSuspending()
                }
            }
        }
    }

    private suspend fun <T> withTransaction(
        pool: Pool,
        block: suspend (SqlConnection) -> T,
    ): T {
        val connection = pool.connection.awaitSuspending()
        try {
            val transaction = connection.begin().awaitSuspending()
            return try {
                val result = block(connection)
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

    private fun RowSet<Row>.first(): Row = firstOrNull() ?: error("Expected at least one row")
}

/**
 * 测试用 outbox 发布器。
 *
 * 该 bean 在测试环境中替换默认日志发布器，用于断言 dispatcher 是否真的把消息交给了 publisher。
 */
@Alternative
@Priority(1)
@ApplicationScoped
class RecordingOutboxMessagePublisher : OutboxMessagePublisher {
    private val _publishedMessages = CopyOnWriteArrayList<StoredOutboxMessage>()

    val publishedMessages: List<StoredOutboxMessage>
        get() = _publishedMessages

    fun clear() {
        _publishedMessages.clear()
    }

    override suspend fun publish(message: StoredOutboxMessage) {
        _publishedMessages += message
    }
}


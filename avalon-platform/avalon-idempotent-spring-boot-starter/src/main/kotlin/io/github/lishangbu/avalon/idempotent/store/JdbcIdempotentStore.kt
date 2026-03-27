package io.github.lishangbu.avalon.idempotent.store

import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * JDBC implementation of [IdempotentStore].
 */
class JdbcIdempotentStore(
    private val jdbcTemplate: JdbcTemplate,
    tableName: String,
    private val clock: Clock = Clock.systemUTC(),
) : IdempotentStore {
    private val tableName = validateTableName(tableName)
    private val recordRowMapper =
        RowMapper { resultSet: ResultSet, _: Int ->
            Record(
                status = resultSet.getString("status"),
                token = resultSet.getString("token"),
                cachedValue = resultSet.getString("cached_value"),
                expiresAt = resultSet.getTimestamp("expires_at").toInstant(),
            )
        }

    override fun acquire(
        key: String,
        token: String,
        processingTtl: Duration,
    ): IdempotentStore.AcquireResult {
        val now = Instant.now(clock)
        val expiresAt = now.plus(processingTtl)
        try {
            jdbcTemplate.update(
                """
                insert into $tableName (
                    idempotency_key,
                    status,
                    token,
                    cached_value,
                    expires_at,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                key,
                STATUS_PROCESSING,
                token,
                null,
                Timestamp.from(expiresAt),
                Timestamp.from(now),
                Timestamp.from(now),
            )
            return IdempotentStore.AcquireResult.Acquired
        } catch (_: DuplicateKeyException) {
            repeat(2) {
                val record = findRecord(key) ?: return IdempotentStore.AcquireResult.Acquired
                if (record.expiresAt <= now) {
                    val updated =
                        jdbcTemplate.update(
                            """
                            update $tableName
                            set status = ?, token = ?, cached_value = ?, expires_at = ?, updated_at = ?
                            where idempotency_key = ? and expires_at <= ?
                            """.trimIndent(),
                            STATUS_PROCESSING,
                            token,
                            null,
                            Timestamp.from(expiresAt),
                            Timestamp.from(now),
                            key,
                            Timestamp.from(now),
                        )
                    if (updated == 1) {
                        return IdempotentStore.AcquireResult.Acquired
                    }
                } else {
                    return when (record.status) {
                        STATUS_PROCESSING -> IdempotentStore.AcquireResult.Processing
                        STATUS_SUCCEEDED -> IdempotentStore.AcquireResult.Completed(record.cachedValue)
                        else -> IdempotentStore.AcquireResult.Processing
                    }
                }
            }
        }
        return IdempotentStore.AcquireResult.Processing
    }

    override fun complete(
        key: String,
        token: String,
        cachedValue: String?,
        ttl: Duration,
    ): Boolean {
        val now = Instant.now(clock)
        val expiresAt = now.plus(ttl)
        return jdbcTemplate.update(
            """
            update $tableName
            set status = ?, cached_value = ?, expires_at = ?, updated_at = ?
            where idempotency_key = ? and status = ? and token = ?
            """.trimIndent(),
            STATUS_SUCCEEDED,
            cachedValue,
            Timestamp.from(expiresAt),
            Timestamp.from(now),
            key,
            STATUS_PROCESSING,
            token,
        ) == 1
    }

    override fun release(
        key: String,
        token: String,
    ): Boolean =
        jdbcTemplate.update(
            "delete from $tableName where idempotency_key = ? and status = ? and token = ?",
            key,
            STATUS_PROCESSING,
            token,
        ) == 1

    override fun renew(
        key: String,
        token: String,
        processingTtl: Duration,
    ): Boolean {
        val now = Instant.now(clock)
        return jdbcTemplate.update(
            """
            update $tableName
            set expires_at = ?, updated_at = ?
            where idempotency_key = ? and status = ? and token = ?
            """.trimIndent(),
            Timestamp.from(now.plus(processingTtl)),
            Timestamp.from(now),
            key,
            STATUS_PROCESSING,
            token,
        ) == 1
    }

    private fun findRecord(key: String): Record? =
        jdbcTemplate
            .query(
                """
                select status, token, cached_value, expires_at
                from $tableName
                where idempotency_key = ?
                """.trimIndent(),
                recordRowMapper,
                key,
            ).firstOrNull()

    private fun validateTableName(tableName: String): String {
        require(TABLE_NAME_PATTERN.matches(tableName)) {
            "Invalid JDBC idempotent table name: $tableName"
        }
        return tableName
    }

    private data class Record(
        val status: String,
        val token: String,
        val cachedValue: String?,
        val expiresAt: Instant,
    )

    companion object {
        const val STATUS_PROCESSING: String = "PROCESSING"
        const val STATUS_SUCCEEDED: String = "SUCCEEDED"

        val TABLE_NAME_PATTERN: Regex = Regex("[A-Za-z0-9_]+")
    }
}

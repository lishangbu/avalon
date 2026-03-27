package io.github.lishangbu.avalon.oauth2.authorizationserver.login

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * JDBC-backed login failure tracker.
 */
class JdbcLoginFailureTracker(
    properties: Oauth2Properties?,
    private val jdbcTemplate: JdbcTemplate,
    transactionManager: PlatformTransactionManager,
    clock: Clock = Clock.systemUTC(),
) : AbstractLoginFailureTracker(properties, clock) {
    private val tableName = validateTableName(properties?.loginFailureTrackerJdbcTableName ?: DEFAULT_TABLE_NAME)
    private val transactionTemplate = TransactionTemplate(transactionManager)
    private val rowMapper =
        RowMapper { resultSet: ResultSet, _: Int ->
            LoginFailureState(
                failures = resultSet.getInt("failure_count"),
                lockUntil = resultSet.getTimestamp("lock_until")?.toInstant(),
            )
        }

    override fun getRemainingLock(username: String?): Duration? {
        if (!isEnabled()) {
            return null
        }
        val key = normalize(username) ?: return null
        val state = findState(key) ?: return null
        val currentTime = now()
        val remainingLock = getRemainingLock(state, currentTime)
        if (remainingLock == null && state.lockUntil != null) {
            jdbcTemplate.update(
                "delete from $tableName where username = ? and lock_until <= ?",
                key,
                Timestamp.from(currentTime),
            )
        }
        return remainingLock
    }

    override fun onFailure(username: String?) {
        if (!isEnabled()) {
            return
        }
        val key = normalize(username) ?: return
        repeat(2) { attempt ->
            try {
                transactionTemplate.executeWithoutResult {
                    val currentTime = now()
                    val state = findStateForUpdate(key)
                    if (state == null) {
                        insertState(key, nextState(null, currentTime), currentTime)
                        return@executeWithoutResult
                    }
                    if (getRemainingLock(state, currentTime) != null) {
                        return@executeWithoutResult
                    }
                    updateState(key, nextState(state, currentTime), currentTime)
                }
                return
            } catch (ex: DuplicateKeyException) {
                if (attempt == 1) {
                    throw ex
                }
            }
        }
    }

    override fun onSuccess(username: String?) {
        val key = normalize(username) ?: return
        jdbcTemplate.update("delete from $tableName where username = ?", key)
    }

    private fun findState(username: String): LoginFailureState? =
        jdbcTemplate
            .query(
                """
                select failure_count, lock_until
                from $tableName
                where username = ?
                """.trimIndent(),
                rowMapper,
                username,
            ).firstOrNull()

    private fun findStateForUpdate(username: String): LoginFailureState? =
        jdbcTemplate
            .query(
                """
                select failure_count, lock_until
                from $tableName
                where username = ?
                for update
                """.trimIndent(),
                rowMapper,
                username,
            ).firstOrNull()

    private fun insertState(
        username: String,
        state: LoginFailureState,
        currentTime: Instant,
    ) {
        jdbcTemplate.update(
            """
            insert into $tableName (
                username,
                failure_count,
                lock_until,
                created_at,
                updated_at
            ) values (?, ?, ?, ?, ?)
            """.trimIndent(),
            username,
            state.failures,
            state.lockUntil?.let(Timestamp::from),
            Timestamp.from(currentTime),
            Timestamp.from(currentTime),
        )
    }

    private fun updateState(
        username: String,
        state: LoginFailureState,
        currentTime: Instant,
    ) {
        jdbcTemplate.update(
            """
            update $tableName
            set failure_count = ?, lock_until = ?, updated_at = ?
            where username = ?
            """.trimIndent(),
            state.failures,
            state.lockUntil?.let(Timestamp::from),
            Timestamp.from(currentTime),
            username,
        )
    }

    private fun validateTableName(tableName: String): String {
        require(TABLE_NAME_PATTERN.matches(tableName)) {
            "Invalid JDBC login failure tracker table name: $tableName"
        }
        return tableName
    }

    companion object {
        const val DEFAULT_TABLE_NAME: String = "oauth2_login_failure"

        val TABLE_NAME_PATTERN: Regex = Regex("[A-Za-z0-9_]+")
    }
}

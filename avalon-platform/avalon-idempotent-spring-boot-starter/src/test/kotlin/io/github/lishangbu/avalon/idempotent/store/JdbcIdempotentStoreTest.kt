package io.github.lishangbu.avalon.idempotent.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class JdbcIdempotentStoreTest {
    private val clock = MutableClock(Instant.parse("2026-03-27T00:00:00Z"))
    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var store: JdbcIdempotentStore

    @BeforeEach
    fun setUp() {
        jdbcTemplate =
            JdbcTemplate(
                DriverManagerDataSource().apply {
                    setDriverClassName("org.h2.Driver")
                    url = "jdbc:h2:mem:idempotent-store;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                    username = "sa"
                },
            )
        jdbcTemplate.execute("drop table if exists idempotency_record")
        jdbcTemplate.execute(
            """
            create table idempotency_record (
                idempotency_key varchar(255) primary key,
                status varchar(32) not null,
                token varchar(128) not null,
                cached_value clob,
                expires_at timestamp not null,
                created_at timestamp not null,
                updated_at timestamp not null
            )
            """.trimIndent(),
        )
        store = JdbcIdempotentStore(jdbcTemplate, "idempotency_record", clock)
    }

    @Test
    fun returnsCachedValueAfterCompletion() {
        val acquired = store.acquire("K-1", "T-1", Duration.ofMinutes(5))
        val completed = store.complete("K-1", "T-1", """{"status":"OK"}""", Duration.ofHours(1))
        val duplicate = store.acquire("K-1", "T-2", Duration.ofMinutes(5))

        assertThat(acquired).isEqualTo(IdempotentStore.AcquireResult.Acquired)
        assertThat(completed).isTrue()
        assertThat(duplicate).isEqualTo(IdempotentStore.AcquireResult.Completed("""{"status":"OK"}"""))
    }

    @Test
    fun releasedRecordCanBeAcquiredAgain() {
        store.acquire("K-2", "T-1", Duration.ofMinutes(5))
        val released = store.release("K-2", "T-1")
        val reacquired = store.acquire("K-2", "T-2", Duration.ofMinutes(5))

        assertThat(released).isTrue()
        assertThat(reacquired).isEqualTo(IdempotentStore.AcquireResult.Acquired)
    }

    @Test
    fun expiredRecordCanBeTakenOver() {
        store.acquire("K-3", "T-1", Duration.ofMinutes(5))
        clock.advance(Duration.ofMinutes(6))

        val reacquired = store.acquire("K-3", "T-2", Duration.ofMinutes(5))

        assertThat(reacquired).isEqualTo(IdempotentStore.AcquireResult.Acquired)
    }

    @Test
    fun renewExtendsProcessingExpiry() {
        store.acquire("K-4", "T-1", Duration.ofMinutes(5))
        val originalExpiry =
            jdbcTemplate
                .queryForObject(
                    "select expires_at from idempotency_record where idempotency_key = ?",
                    Timestamp::class.java,
                    "K-4",
                )!!
                .toInstant()

        clock.advance(Duration.ofMinutes(2))
        val renewed = store.renew("K-4", "T-1", Duration.ofMinutes(5))
        val renewedExpiry =
            jdbcTemplate
                .queryForObject(
                    "select expires_at from idempotency_record where idempotency_key = ?",
                    Timestamp::class.java,
                    "K-4",
                )!!
                .toInstant()

        assertThat(renewed).isTrue()
        assertThat(renewedExpiry).isAfter(originalExpiry)
    }

    private class MutableClock(
        private var instant: Instant,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("UTC")

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = instant

        fun advance(duration: Duration) {
            instant = instant.plus(duration)
        }
    }
}

package io.github.lishangbu.avalon.oauth2.authorizationserver.login

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource

class JdbcLoginFailureTrackerTest {
    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var transactionManager: DataSourceTransactionManager

    @BeforeEach
    fun setUp() {
        val dataSource =
            DriverManagerDataSource().apply {
                setDriverClassName("org.h2.Driver")
                url = "jdbc:h2:mem:login-failure-tracker;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                username = "sa"
            }

        jdbcTemplate = JdbcTemplate(dataSource)
        transactionManager = DataSourceTransactionManager(dataSource)
        jdbcTemplate.execute("drop table if exists oauth2_login_failure")
        jdbcTemplate.execute(
            """
            create table oauth2_login_failure (
                username varchar(255) primary key,
                failure_count integer not null,
                lock_until timestamp null,
                created_at timestamp not null,
                updated_at timestamp not null
            )
            """.trimIndent(),
        )
    }

    @Test
    fun tracksFailuresAndLocksUser() {
        val properties =
            Oauth2Properties().apply {
                maxLoginFailures = 2
                setLoginLockDuration("80ms")
            }
        val tracker = JdbcLoginFailureTracker(properties, jdbcTemplate, transactionManager)

        assertTrue(tracker.isEnabled())
        tracker.onFailure("user")
        assertNull(tracker.getRemainingLock("user"))

        tracker.onFailure("user")
        assertNotNull(tracker.getRemainingLock("user"))

        Thread.sleep(120)
        assertNull(tracker.getRemainingLock("user"))
    }

    @Test
    fun clearsStateOnSuccess() {
        val properties =
            Oauth2Properties().apply {
                maxLoginFailures = 1
                setLoginLockDuration("200ms")
            }
        val tracker = JdbcLoginFailureTracker(properties, jdbcTemplate, transactionManager)

        tracker.onFailure("user")
        assertNotNull(tracker.getRemainingLock("user"))

        tracker.onSuccess("user")
        assertNull(tracker.getRemainingLock("user"))
    }

    @Test
    fun rejectsInvalidTableName() {
        val properties =
            Oauth2Properties().apply {
                loginFailureTrackerJdbcTableName = "oauth2-login-failure"
            }

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                JdbcLoginFailureTracker(properties, jdbcTemplate, transactionManager)
            }

        assertTrue(exception.message!!.contains("Invalid JDBC login failure tracker table name"))
    }

    @Test
    fun disabledWhenPropertiesInvalid() {
        val properties =
            Oauth2Properties().apply {
                maxLoginFailures = 0
                setLoginLockDuration("80ms")
            }
        val tracker = JdbcLoginFailureTracker(properties, jdbcTemplate, transactionManager)

        assertFalse(tracker.isEnabled())
        tracker.onFailure("user")
        assertNull(tracker.getRemainingLock("user"))
    }
}

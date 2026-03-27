package io.github.lishangbu.avalon.idempotent.aspect

import io.github.lishangbu.avalon.idempotent.annotation.Idempotent
import io.github.lishangbu.avalon.idempotent.autoconfigure.IdempotentAutoConfiguration
import io.github.lishangbu.avalon.idempotent.exception.IdempotentConflictException
import io.github.lishangbu.avalon.idempotent.exception.IdempotentConflictState
import io.github.lishangbu.avalon.idempotent.store.IdempotentStore
import io.github.lishangbu.avalon.idempotent.support.DuplicateStrategy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class IdempotentAspectTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    AopAutoConfiguration::class.java,
                    IdempotentAutoConfiguration::class.java,
                ),
            ).withUserConfiguration(TestConfiguration::class.java)

    @Test
    fun rejectsRepeatedCompletedRequestByDefault() {
        contextRunner.run { context ->
            val service = context.getBean(RejectingService::class.java)

            assertThat(service.create("A-1")).isEqualTo("created:A-1")
            assertThatThrownBy { service.create("A-1") }
                .isInstanceOfSatisfying(IdempotentConflictException::class.java) { ex ->
                    assertThat(ex.state).isEqualTo(IdempotentConflictState.COMPLETED)
                }
            assertThat(service.calls()).isEqualTo(1)
        }
    }

    @Test
    fun returnsCachedResultWhenConfigured() {
        contextRunner.run { context ->
            val service = context.getBean(CachedService::class.java)

            val first = service.create("B-1")
            val second = service.create("B-1")

            assertThat(first).isEqualTo("cached:B-1")
            assertThat(second).isEqualTo("cached:B-1")
            assertThat(service.calls()).isEqualTo(1)
        }
    }

    @Test
    fun releasesKeyAfterFailureSoRetryCanProceed() {
        contextRunner.run { context ->
            val service = context.getBean(FailingService::class.java)

            assertThatThrownBy { service.create("C-1") }.isInstanceOf(IllegalStateException::class.java)
            assertThat(service.create("C-1")).isEqualTo("recovered:C-1")
            assertThat(service.calls()).isEqualTo(2)
        }
    }

    @Test
    fun readsIdempotencyKeyFromRequestHeaderWhenExpressionIsOmitted() {
        contextRunner.run { context ->
            val service = context.getBean(HeaderFallbackService::class.java)
            RequestContextHolder.setRequestAttributes(
                ServletRequestAttributes(
                    MockHttpServletRequest().apply {
                        addHeader("Idempotency-Key", "HDR-1")
                    },
                ),
            )

            try {
                assertThat(service.create()).isEqualTo("header")
                assertThatThrownBy { service.create() }
                    .isInstanceOfSatisfying(IdempotentConflictException::class.java) { ex ->
                        assertThat(ex.state).isEqualTo(IdempotentConflictState.COMPLETED)
                    }
                assertThat(service.calls()).isEqualTo(1)
            } finally {
                RequestContextHolder.resetRequestAttributes()
            }
        }
    }

    @Test
    fun renewsLeaseForLongRunningExecution() {
        contextRunner
            .withPropertyValues(
                "idempotent.processing-ttl=300ms",
                "idempotent.renew-interval=100ms",
            ).run { context ->
                val service = context.getBean(SlowService::class.java)
                val store = context.getBean(IdempotentStore::class.java) as InMemoryIdempotentStore

                assertThat(service.create("R-1")).isEqualTo("slow:R-1")
                assertThat(store.renewCount()).isGreaterThan(0)
            }
    }

    @Configuration(proxyBeanMethods = false)
    class TestConfiguration {
        @Bean
        fun idempotentStore(): IdempotentStore = InMemoryIdempotentStore()

        @Bean
        fun rejectingService(): RejectingService = RejectingService()

        @Bean
        fun cachedService(): CachedService = CachedService()

        @Bean
        fun failingService(): FailingService = FailingService()

        @Bean
        fun headerFallbackService(): HeaderFallbackService = HeaderFallbackService()

        @Bean
        fun slowService(): SlowService = SlowService()
    }

    @Service
    open class RejectingService {
        private val calls = AtomicInteger()

        @Idempotent(key = "#orderNo", prefix = "reject")
        open fun create(orderNo: String): String {
            calls.incrementAndGet()
            return "created:$orderNo"
        }

        fun calls(): Int = calls.get()
    }

    @Service
    open class CachedService {
        private val calls = AtomicInteger()

        @Idempotent(
            key = "#orderNo",
            prefix = "cached",
            duplicateStrategy = DuplicateStrategy.RETURN_CACHED,
        )
        open fun create(orderNo: String): String {
            calls.incrementAndGet()
            return "cached:$orderNo"
        }

        fun calls(): Int = calls.get()
    }

    @Service
    open class FailingService {
        private val calls = AtomicInteger()

        @Idempotent(key = "#orderNo", prefix = "failure")
        open fun create(orderNo: String): String {
            val attempt = calls.incrementAndGet()
            check(attempt > 1) { "boom" }
            return "recovered:$orderNo"
        }

        fun calls(): Int = calls.get()
    }

    @Service
    open class HeaderFallbackService {
        private val calls = AtomicInteger()

        @Idempotent(prefix = "header")
        open fun create(): String {
            calls.incrementAndGet()
            return "header"
        }

        fun calls(): Int = calls.get()
    }

    @Service
    open class SlowService {
        @Idempotent(key = "#orderNo", prefix = "slow")
        open fun create(orderNo: String): String {
            Thread.sleep(350)
            return "slow:$orderNo"
        }
    }

    private class InMemoryIdempotentStore : IdempotentStore {
        private val values = ConcurrentHashMap<String, StoredValue>()
        private val renewCount = AtomicInteger()

        override fun acquire(
            key: String,
            token: String,
            processingTtl: Duration,
        ): IdempotentStore.AcquireResult {
            val newValue = StoredValue.processing(token)
            val existing = values.putIfAbsent(key, newValue) ?: return IdempotentStore.AcquireResult.Acquired
            return when (existing.status) {
                Status.PROCESSING -> IdempotentStore.AcquireResult.Processing
                Status.SUCCEEDED -> IdempotentStore.AcquireResult.Completed(existing.cachedValue)
            }
        }

        override fun complete(
            key: String,
            token: String,
            cachedValue: String?,
            ttl: Duration,
        ): Boolean {
            val current = values[key] ?: return false
            if (current.status != Status.PROCESSING || current.token != token) {
                return false
            }
            values[key] = StoredValue.succeeded(token, cachedValue)
            return true
        }

        override fun release(
            key: String,
            token: String,
        ): Boolean {
            val current = values[key] ?: return false
            if (current.token != token) {
                return false
            }
            values.remove(key)
            return true
        }

        override fun renew(
            key: String,
            token: String,
            processingTtl: Duration,
        ): Boolean {
            val current = values[key] ?: return false
            if (current.token != token || current.status != Status.PROCESSING) {
                return false
            }
            renewCount.incrementAndGet()
            return true
        }

        fun renewCount(): Int = renewCount.get()

        private data class StoredValue(
            val status: Status,
            val token: String,
            val cachedValue: String? = null,
        ) {
            companion object {
                fun processing(token: String): StoredValue = StoredValue(Status.PROCESSING, token)

                fun succeeded(
                    token: String,
                    cachedValue: String?,
                ): StoredValue = StoredValue(Status.SUCCEEDED, token, cachedValue)
            }
        }

        private enum class Status {
            PROCESSING,
            SUCCEEDED,
        }
    }
}

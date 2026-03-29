package io.github.lishangbu.avalon.idempotent.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.script.RedisScript
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

class RedisIdempotentStoreTest {
    private lateinit var template: StringRedisTemplate
    private lateinit var valueOperations: ValueOperations<String, String>
    private lateinit var store: RedisIdempotentStore

    @BeforeEach
    fun setUp() {
        @Suppress("UNCHECKED_CAST")
        val mockedValueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>
        valueOperations = mockedValueOperations
        template = mock(StringRedisTemplate::class.java)
        Mockito.`when`(template.opsForValue()).thenReturn(valueOperations)
        store = RedisIdempotentStore(template, JsonMapper.builder().build())
    }

    @Test
    fun returnsAcquiredWhenKeyIsAbsent() {
        Mockito
            .`when`(valueOperations.setIfAbsent(Mockito.eq("K-1"), anyString(), Mockito.eq(Duration.ofMinutes(5))))
            .thenReturn(true)

        val result = store.acquire("K-1", "T-1", Duration.ofMinutes(5))

        assertThat(result).isEqualTo(IdempotentStore.AcquireResult.Acquired)
    }

    @Test
    fun returnsProcessingWhenExistingValueIsStillProcessing() {
        Mockito
            .`when`(valueOperations.setIfAbsent(Mockito.eq("K-2"), anyString(), Mockito.eq(Duration.ofMinutes(5))))
            .thenReturn(false)
        Mockito
            .`when`(valueOperations.get("K-2"))
            .thenReturn("""{"status":"PROCESSING","token":"T-0"}""")

        val result = store.acquire("K-2", "T-1", Duration.ofMinutes(5))

        assertThat(result).isEqualTo(IdempotentStore.AcquireResult.Processing)
    }

    @Test
    fun returnsCompletedWhenExistingValueHasSucceeded() {
        Mockito
            .`when`(valueOperations.setIfAbsent(Mockito.eq("K-3"), anyString(), Mockito.eq(Duration.ofMinutes(5))))
            .thenReturn(false)
        Mockito
            .`when`(valueOperations.get("K-3"))
            .thenReturn("""{"status":"SUCCEEDED","token":"T-0","cachedValue":"{\"ok\":true}"}""")

        val result = store.acquire("K-3", "T-1", Duration.ofMinutes(5))

        assertThat(result).isEqualTo(IdempotentStore.AcquireResult.Completed("""{"ok":true}"""))
    }

    @Test
    fun retriesAcquisitionWhenValueDisappearsBetweenReads() {
        Mockito
            .`when`(valueOperations.setIfAbsent(Mockito.eq("K-4"), anyString(), Mockito.eq(Duration.ofMinutes(5))))
            .thenReturn(false, true)
        Mockito
            .`when`(valueOperations.get("K-4"))
            .thenReturn(null as String?)

        val result = store.acquire("K-4", "T-1", Duration.ofMinutes(5))

        assertThat(result).isEqualTo(IdempotentStore.AcquireResult.Acquired)
    }

    @Test
    fun returnsProcessingWhenRetriesAreExhausted() {
        Mockito
            .`when`(valueOperations.setIfAbsent(Mockito.eq("K-5"), anyString(), Mockito.eq(Duration.ofMinutes(5))))
            .thenReturn(false, false, false)
        Mockito
            .`when`(valueOperations.get("K-5"))
            .thenReturn(null as String?)

        val result = store.acquire("K-5", "T-1", Duration.ofMinutes(5))

        assertThat(result).isEqualTo(IdempotentStore.AcquireResult.Processing)
    }

    @Test
    fun delegatesCompleteReleaseAndRenewToRedisScripts() {
        Mockito
            .`when`(template.execute(anyLongScript(), anyStringKeys(), Mockito.eq("T-1"), anyString(), Mockito.eq("3600000")))
            .thenReturn(1L)
        Mockito
            .`when`(template.execute(anyLongScript(), anyStringKeys(), Mockito.eq("T-1")))
            .thenReturn(1L)
        Mockito
            .`when`(template.execute(anyLongScript(), anyStringKeys(), Mockito.eq("T-1"), Mockito.eq("300000")))
            .thenReturn(1L)

        val completed = store.complete("K-6", "T-1", """{"done":true}""", Duration.ofHours(1))
        assertThat(completed).isTrue()
        Mockito
            .verify(template)
            .execute(anyLongScript(), Mockito.eq(mutableListOf("K-6")), Mockito.eq("T-1"), anyString(), Mockito.eq("3600000"))

        val released = store.release("K-6", "T-1")
        assertThat(released).isTrue()
        Mockito.verify(template).execute(anyLongScript(), Mockito.eq(mutableListOf("K-6")), Mockito.eq("T-1"))

        val renewed = store.renew("K-6", "T-1", Duration.ofMinutes(5))
        assertThat(renewed).isTrue()
        Mockito.verify(template).execute(anyLongScript(), Mockito.eq(mutableListOf("K-6")), Mockito.eq("T-1"), Mockito.eq("300000"))
    }

    @Test
    fun returnsFalseWhenRedisScriptDoesNotAcknowledgeMutation() {
        Mockito
            .`when`(template.execute(anyLongScript(), anyStringKeys(), Mockito.eq("T-1"), anyString(), Mockito.eq("300000")))
            .thenReturn(0L)
        Mockito
            .`when`(template.execute(anyLongScript(), anyStringKeys(), Mockito.eq("T-1")))
            .thenReturn(0L)
        Mockito
            .`when`(template.execute(anyLongScript(), anyStringKeys(), Mockito.eq("T-1"), Mockito.eq("300000")))
            .thenReturn(0L)

        assertThat(store.complete("K-7", "T-1", null, Duration.ofMinutes(5))).isFalse()
        assertThat(store.release("K-7", "T-1")).isFalse()
        assertThat(store.renew("K-7", "T-1", Duration.ofMinutes(5))).isFalse()
    }

    private fun anyString(): String = Mockito.anyString() ?: ""

    @Suppress("UNCHECKED_CAST")
    private fun anyLongScript(): RedisScript<Long> {
        Mockito.any(RedisScript::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyStringKeys(): MutableList<String> {
        Mockito.anyList<String>()
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}

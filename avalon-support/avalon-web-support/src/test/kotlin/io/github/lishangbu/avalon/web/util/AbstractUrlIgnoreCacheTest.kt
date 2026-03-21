package io.github.lishangbu.avalon.web.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** AbstractUrlIgnoreCache 单元测试 创建一个匿名子类来实现抽象方法 测试匹配的 URL 测试不匹配的 URL */
class AbstractUrlIgnoreCacheTest {
    private lateinit var urlIgnoreCache: AbstractUrlIgnoreCache

    @BeforeEach
    fun setUp() {
        urlIgnoreCache =
            object : AbstractUrlIgnoreCache() {
                override fun getIgnoreUrls(): List<String> = listOf("/api/ignore/**", "/public/**")
            }
    }

    @Test
    fun testShouldIgnoreMatchingUrl() {
        assertTrue(urlIgnoreCache.shouldIgnore("/api/ignore/test"))
        assertTrue(urlIgnoreCache.shouldIgnore("/public/test"))
    }

    @Test
    fun testShouldIgnoreNonMatchingUrl() {
        assertFalse(urlIgnoreCache.shouldIgnore("/api/test"))
        assertFalse(urlIgnoreCache.shouldIgnore("/private/test"))
    }
}

package io.github.lishangbu.avalon.web.util

import org.springframework.util.AntPathMatcher
import org.springframework.util.ConcurrentLruCache

/**
 * 忽略 URL 缓存基类
 *
 * 使用 LRU 缓存保存 URL 是否命中忽略规则的结果
 */
abstract class AbstractUrlIgnoreCache {
    /** ANT 路径匹配器 */
    private val antPathMatcher = AntPathMatcher()

    /** 缓存 */
    private val cache = ConcurrentLruCache<String, Boolean>(1024, this::urlShouldBeIgnored)

    /** 判断 URL 是否需要忽略 */
    fun shouldIgnore(url: String): Boolean = cache.get(url)

    /** 获取忽略 URL 列表 */
    protected abstract fun getIgnoreUrls(): List<String>

    /** 执行忽略规则匹配 */
    private fun urlShouldBeIgnored(url: String): Boolean = getIgnoreUrls().any { pattern -> antPathMatcher.match(pattern, url) }
}

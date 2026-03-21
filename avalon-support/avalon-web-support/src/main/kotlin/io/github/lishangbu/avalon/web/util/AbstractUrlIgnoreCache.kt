package io.github.lishangbu.avalon.web.util

import org.springframework.util.AntPathMatcher
import org.springframework.util.ConcurrentLruCache

/**
 * 忽略 URL 缓存基类 提供基于 LRU 缓存的 URL 忽略功能，使用 AntPathMatcher 进行路径匹配 子类需要实现 `getIgnoreUrls` 方法以提供忽略的 URL 列表
 * 主要功能：
 * - 缓存 URL 匹配结果以提高性能
 * - 支持基于通配符的路径匹配
 *
 * @param url 要检查的 URL
 * @return 如果 URL 应被忽略，则返回 true；否则返回 false 获取忽略的 URL 列表（由子类实现）
 * @return 忽略的 URL 列表 判断 URL 是否匹配忽略规则
 * @return 如果 URL 匹配忽略规则，则返回 true；否则返回 false
 * @author lishangbu
 * @since 2024/2/7 LRU 缓存，用于存储 URL 匹配结果，键为 URL，值为是否忽略的布尔值 路径匹配器，用于支持通配符的路径匹配 构造方法，初始化 LRU 缓存（默认容量
 *   1024） 判断给定的 URL 是否应被忽略
 */

/** 忽略 URL 缓存基类。 */
abstract class AbstractUrlIgnoreCache {
    private val antPathMatcher = AntPathMatcher()
    private val cache = ConcurrentLruCache<String, Boolean>(1024, this::urlShouldBeIgnored)

    fun shouldIgnore(url: String): Boolean = cache.get(url)

    protected abstract fun getIgnoreUrls(): List<String>

    private fun urlShouldBeIgnored(url: String): Boolean = getIgnoreUrls().any { pattern -> antPathMatcher.match(pattern, url) }
}

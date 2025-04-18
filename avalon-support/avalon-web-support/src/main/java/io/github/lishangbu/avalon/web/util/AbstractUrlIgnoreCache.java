package io.github.lishangbu.avalon.web.util;

import java.util.List;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ConcurrentLruCache;

/**
 * 忽略URL 缓存基类。
 *
 * <p>该类提供了基于 LRU 缓存的 URL 忽略功能，使用 `AntPathMatcher` 进行路径匹配。 子类需要实现 `getIgnoreUrls` 方法以提供忽略的 URL 列表。
 *
 * <p>主要功能：
 *
 * <ul>
 *   <li>缓存 URL 匹配结果以提高性能。
 *   <li>支持基于通配符的路径匹配。
 * </ul>
 *
 * @author lishangbu
 * @since 2024/2/7
 */
public abstract class AbstractUrlIgnoreCache {

  /**
   * LRU 缓存，用于存储 URL 匹配结果。
   *
   * <p>缓存的键为 URL，值为是否忽略的布尔值。
   */
  private final ConcurrentLruCache<String, Boolean> cache;

  /** 路径匹配器，用于支持通配符的路径匹配。 */
  private final AntPathMatcher antPathMatcher = new AntPathMatcher();

  /**
   * 构造方法，初始化 LRU 缓存。
   *
   * <p>缓存的最大容量为 1024。
   */
  public AbstractUrlIgnoreCache() {
    this.cache = new ConcurrentLruCache<>(1024, this::urlShouldBeIgnored);
  }

  /**
   * 判断给定的 URL 是否应被忽略。
   *
   * @param url 要检查的 URL
   * @return 如果 URL 应被忽略，则返回 true；否则返回 false
   */
  public boolean shouldIgnore(String url) {
    return cache.get(url);
  }

  /**
   * 获取忽略的 URL 列表。
   *
   * <p>该方法由子类实现，通常从配置文件或数据库中加载忽略的 URL。
   *
   * @return 忽略的 URL 列表
   */
  protected abstract List<String> getIgnoreUrls();

  /**
   * 判断 URL 是否匹配忽略规则。
   *
   * @param url 要检查的 URL
   * @return 如果 URL 匹配忽略规则，则返回 true；否则返回 false
   */
  private boolean urlShouldBeIgnored(String url) {
    List<String> ignoreUrls = getIgnoreUrls();
    return ignoreUrls.stream().anyMatch(pattern -> antPathMatcher.match(pattern, url));
  }
}

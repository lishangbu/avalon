package io.github.lishangbu.avalon.web.util;

import java.util.List;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ConcurrentLruCache;

/**
 * URL 忽略缓存
 *
 * @author lishangbu
 * @since 2024/2/7
 */
public abstract class AbstractUrlIgnoreCache {

  private final ConcurrentLruCache<String, Boolean> cache;

  private final AntPathMatcher antPathMatcher = new AntPathMatcher();

  public AbstractUrlIgnoreCache() {
    // 最大容量为1024
    this.cache = new ConcurrentLruCache<>(1024, this::urlShouldBeIgnored);
  }

  public boolean shouldIgnore(String url) {
    return cache.get(url);
  }

  // 一个抽象方法，用于获取ignoreUrls列表
  // 实际应用中，这个列表可能来自配置文件或数据库
  protected abstract List<String> getIgnoreUrls();

  private boolean urlShouldBeIgnored(String url) {
    List<String> ignoreUrls = getIgnoreUrls();
    return ignoreUrls.stream().anyMatch(pattern -> antPathMatcher.match(pattern, url));
  }
}

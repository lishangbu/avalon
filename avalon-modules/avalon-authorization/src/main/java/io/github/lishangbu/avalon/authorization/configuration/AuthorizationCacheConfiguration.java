package io.github.lishangbu.avalon.authorization.configuration;

import static io.github.lishangbu.avalon.authorization.constant.AuthorizationCacheConstants.CAFFEINE_CACHE_BEAN_NAME;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 认证模块缓存配置
 *
 * @author lishangbu
 * @since 2025/8/23
 */
@Slf4j
@EnableCaching
@Configuration
public class AuthorizationCacheConfiguration {

  /**
   * 配置缓存管理器
   *
   * @return 缓存管理器
   */
  @Bean(CAFFEINE_CACHE_BEAN_NAME)
  @ConditionalOnMissingBean(name = CAFFEINE_CACHE_BEAN_NAME)
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(
        Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            // 初始的缓存空间大小
            .initialCapacity(128)
            // 缓存的最大条数
            .maximumSize(2_048));
    return cacheManager;
  }
}

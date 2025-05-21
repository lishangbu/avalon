package io.github.lishangbu.avalon.pokeapi.autoconfiguration;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存自动装配
 *
 * @author lishangbu
 * @since 2025/5/21
 */
@Configuration
@EnableCaching
public class PokeApiCacheAutoConfiguration {
  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(
        Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(2048));
    return cacheManager;
  }
}

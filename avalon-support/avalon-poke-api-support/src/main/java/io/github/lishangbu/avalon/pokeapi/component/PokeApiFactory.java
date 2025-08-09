package io.github.lishangbu.avalon.pokeapi.component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * POKE API工厂
 *
 * @author lishangbu
 * @since 2025/5/22
 */
public class PokeApiFactory {
  private static final Logger log = LoggerFactory.getLogger(PokeApiFactory.class);

  // 工厂方法参数配置
  private static final int PAGE_CACHE_EXPIRE_MINUTES = 10;
  private static final int PAGE_CACHE_MAX_SIZE = 1000;
  private static final int SINGLE_CACHE_EXPIRE_MINUTES = 30;
  private static final int SINGLE_CACHE_MAX_SIZE = 2000;

  /** 分页资源缓存10分钟自动失效，最大1000条。 */
  private final Cache<String, NamedAPIResourceList> pagedResourceCache =
      Caffeine.newBuilder()
          .expireAfterWrite(PAGE_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
          .maximumSize(PAGE_CACHE_MAX_SIZE)
          .build();

  /** 单体资源缓存30分钟自动失效，最大2000条。 */
  private final Cache<String, Object> singleResourceCache =
      Caffeine.newBuilder()
          .expireAfterWrite(SINGLE_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
          .maximumSize(SINGLE_CACHE_MAX_SIZE)
          .build();

  private final PokeApiService pokeApiService;

  public PokeApiFactory(PokeApiService pokeApiService) {
    this.pokeApiService = pokeApiService;
  }

  /** 生成分页缓存key */
  private String buildPageCacheKey(PokeApiEndpointEnum endpoint) {
    return endpoint.name();
  }

  /** 生成单体缓存key */
  private String buildSingleCacheKey(PokeApiEndpointEnum endpoint, Serializable idOrName) {
    return endpoint.name() + ":" + idOrName;
  }

  /**
   * 根据端点获取分页资源列表，参数校验更健壮
   *
   * @param endpoint 端点枚举
   * @return 资源列表
   */
  public NamedAPIResourceList getPagedResource(PokeApiEndpointEnum endpoint) {
    String cacheKey = buildPageCacheKey(endpoint);
    NamedAPIResourceList cached = pagedResourceCache.getIfPresent(cacheKey);
    if (cached != null) {
      log.debug("分页资源命中缓存: {}", cacheKey);
      return cached;
    }
    log.debug("获取分页资源: endpoint:[{}]", endpoint);
    NamedAPIResourceList result = pokeApiService.listNamedAPIResources(endpoint.getType());
    if (result != null) {
      pagedResourceCache.put(cacheKey, result);
    }
    return result;
  }

  /**
   * 根据端点和参数获取单个资源，类型安全
   *
   * @param endpoint 端点枚举
   * @param id biao shi bie
   * @param <T> 返回类型
   * @return 资源对象
   */
  @SuppressWarnings("unchecked")
  public <T> T getSingleResource(PokeApiEndpointEnum endpoint, Integer id) {
    String cacheKey = buildSingleCacheKey(endpoint, id);
    Object cached = singleResourceCache.getIfPresent(cacheKey);
    if (cached != null) {
      log.debug("单体资源命中缓存: {}", cacheKey);
      return (T) cached;
    }
    log.debug("获取单体资源: endpoint:[{}], id:[{}]", endpoint, id);
    T result =
        (T) pokeApiService.getEntityFromUri(endpoint.getResponseType(), endpoint.getType(), id);
    if (result != null) {
      singleResourceCache.put(cacheKey, result);
    }
    return result;
  }
}

package io.github.lishangbu.avalon.pokeapi.component;

import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import java.io.Serializable;

/**
 * Poke API 请求模板
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public interface PokeApiService {
  /**
   * 获取带有分页信息的命名资源列表
   *
   * @param uri 请求的URI
   * @param offset 偏移量
   * @param limit 返回数量限制
   * @return 命名资源列表
   */
  NamedAPIResourceList listNamedAPIResources(String uri, Integer offset, Integer limit);

  /**
   * 通过指定的URI和参数获取指定类型的数据实体
   *
   * @param responseType 响应数据的类型
   * @param uri 请求的URI模板
   * @param idOrName URI中的参数,可以是ID，也可以是name
   * @return 指定类型的数据实体
   */
  <T> T getEntityFromUri(Class<T> responseType, String uri, Serializable idOrName);
}

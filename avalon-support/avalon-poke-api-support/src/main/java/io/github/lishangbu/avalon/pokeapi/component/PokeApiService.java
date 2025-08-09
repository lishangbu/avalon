package io.github.lishangbu.avalon.pokeapi.component;

import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;

/**
 * Poke API 请求模板
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public interface PokeApiService {
  /**
   * 获取命名资源列表
   *
   * @param type 资源类型
   * @return 命名资源列表
   */
  NamedAPIResourceList listNamedAPIResources(String type);

  /**
   * 通过指定的URI和参数获取指定类型的数据实体
   *
   * @param responseType 响应数据的类型
   * @param type 资源类型
   * @param id 资源对应的唯一ID
   * @return 指定类型的数据实体
   */
  <T> T getEntityFromUri(Class<T> responseType, String type, Integer id);
}

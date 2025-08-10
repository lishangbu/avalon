package io.github.lishangbu.avalon.pokeapi.component;

import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
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
   * @param typeEnum 资源类型枚举
   * @return 命名资源列表
   */
  NamedAPIResourceList listNamedAPIResources(PokeApiDataTypeEnum typeEnum);

  /**
   * 通过指定的URI和参数获取指定类型的数据实体
   *
   * @param typeEnum 资源类型枚举
   * @param id 资源对应的唯一ID
   * @return 指定类型的数据实体
   */
  <T> T getEntityFromUri(PokeApiDataTypeEnum typeEnum, Integer id);
}

package io.github.lishangbu.avalon.pokeapi.service;

import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type;
import java.io.Serializable;

/**
 * 属性服务
 *
 * @author lishangbu
 * @since 2025/5/21
 */
public interface TypeService {
  /**
   * 获取属性类型列表
   *
   * @param offset 偏移量
   * @param limit 返回数量限制
   * @return 属性类型资源列表
   */
  NamedAPIResourceList listTypes(Integer offset, Integer limit);

  /**
   * 根据参数获取属性类型详情
   *
   * @param arg 属性类型的唯一标识（如ID或名称）
   * @return 属性类型详情
   */
  Type getType(Serializable arg);
}

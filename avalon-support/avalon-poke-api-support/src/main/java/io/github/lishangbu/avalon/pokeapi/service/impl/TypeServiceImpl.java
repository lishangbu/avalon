package io.github.lishangbu.avalon.pokeapi.service.impl;

import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type;
import io.github.lishangbu.avalon.pokeapi.service.TypeService;
import java.io.Serializable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 属性服务实现类
 *
 * @author lishangbu
 * @since 2025/5/21
 */
@Service
public class TypeServiceImpl extends AbstractPokeApiService implements TypeService {

  /**
   * 获取属性类型列表
   *
   * @param offset 偏移量
   * @param limit 返回数量限制
   * @return 属性类型资源列表
   */
  @Override
  @Cacheable(value = "types", key = "#offset + '-' + #limit")
  public NamedAPIResourceList listTypes(Integer offset, Integer limit) {
    return listNamedAPIResources("/type", offset, limit);
  }

  /**
   * 根据参数获取属性类型详情
   *
   * @param arg 属性类型的唯一标识（如ID或名称）
   * @return 属性类型详情
   */
  @Override
  @Cacheable(value = "type", key = "#arg")
  public Type getType(Serializable arg) {
    return getEntityFromUri(Type.class, "/type/{arg}", arg);
  }
}

package io.github.lishangbu.avalon.pokeapi.service;

import io.github.lishangbu.avalon.pokeapi.model.berry.Berry;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import java.io.Serializable;

/**
 * 树果服务
 *
 * @author lishangbu
 * @since 2025/5/22
 */
public interface BerryService {
  /**
   * 获取树果列表
   *
   * @param offset 偏移量
   * @param limit 返回数量限制
   * @return 树果资源列表
   */
  NamedAPIResourceList listBerries(Integer offset, Integer limit);

  /**
   * 根据参数获取树果详情
   *
   * @param arg 查询参数
   * @return 树果信息
   */
  Berry getBerry(Serializable arg);
}

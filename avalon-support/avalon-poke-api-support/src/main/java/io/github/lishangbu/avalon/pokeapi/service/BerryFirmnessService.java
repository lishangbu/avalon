package io.github.lishangbu.avalon.pokeapi.service;

import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFirmness;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import java.io.Serializable;

/**
 * 树果硬度服务
 *
 * @author lishangbu
 * @since 2025/5/22
 */
public interface BerryFirmnessService {
  /**
   * 获取树果硬度列表
   *
   * @param offset 偏移量
   * @param limit 返回数量限制
   * @return 树果硬度资源列表
   */
  NamedAPIResourceList listBerryFirmnesses(Integer offset, Integer limit);

  /**
   * 根据参数获取树果硬度详情
   *
   * @param arg 查询参数
   * @return 树果硬度信息
   */
  BerryFirmness getBerryFirmness(Serializable arg);
}

package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import java.util.Optional;

/**
 * 树果硬度(berry_firmness)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface BerryFirmnessMapper {

  /**
   * 通过id查询单条树果硬度数据
   *
   * @param id 主键
   * @return 可选的树果硬度
   */
  Optional<BerryFirmness> selectById(Long id);

  /**
   * 新增树果硬度
   *
   * @param berryFirmness 实例对象
   * @return 影响行数
   */
  int insert(BerryFirmness berryFirmness);

  /**
   * 修改树果硬度
   *
   * @param berryFirmness 实例对象
   * @return 影响行数
   */
  int updateById(BerryFirmness berryFirmness);

  /**
   * 通过id删除树果硬度
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

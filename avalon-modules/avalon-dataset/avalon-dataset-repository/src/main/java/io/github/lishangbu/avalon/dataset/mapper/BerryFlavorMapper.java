package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import java.util.Optional;

/**
 * 树果风味(berry_flavor)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface BerryFlavorMapper {

  /**
   * 通过id查询单条树果风味数据
   *
   * @param id 主键
   * @return 可选的树果风味
   */
  Optional<BerryFlavor> selectById(Long id);

  /**
   * 新增树果风味
   *
   * @param berryFlavor 实例对象
   * @return 影响行数
   */
  int insert(BerryFlavor berryFlavor);

  /**
   * 修改树果风味
   *
   * @param berryFlavor 实例对象
   * @return 影响行数
   */
  int updateById(BerryFlavor berryFlavor);

  /**
   * 通过id删除树果风味
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

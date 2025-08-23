package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.ItemCategory;
import java.util.Optional;

/**
 * 道具类别(item_category)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface ItemCategoryMapper {

  /**
   * 通过id查询单条道具类别数据
   *
   * @param id 主键
   * @return 可选的道具类别
   */
  Optional<ItemCategory> selectById(Long id);

  /**
   * 新增道具类别
   *
   * @param itemCategory 实例对象
   * @return 影响行数
   */
  int insert(ItemCategory itemCategory);

  /**
   * 修改道具类别
   *
   * @param itemCategory 实例对象
   * @return 影响行数
   */
  int updateById(ItemCategory itemCategory);

  /**
   * 通过id删除道具类别
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

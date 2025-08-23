package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.ItemAttribute;
import java.util.Optional;

/**
 * 道具属性(item_attribute)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface ItemAttributeMapper {

  /**
   * 通过id查询单条道具属性数据
   *
   * @param id 主键
   * @return 可选的道具属性
   */
  Optional<ItemAttribute> selectById(Long id);

  /**
   * 新增道具属性
   *
   * @param itemAttribute 实例对象
   * @return 影响行数
   */
  int insert(ItemAttribute itemAttribute);

  /**
   * 修改道具属性
   *
   * @param itemAttribute 实例对象
   * @return 影响行数
   */
  int updateById(ItemAttribute itemAttribute);

  /**
   * 通过id删除道具属性
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

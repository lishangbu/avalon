package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.ItemAttributeRelation;
import java.util.Optional;

/**
 * 道具属性关系(item_attribute_relation)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface ItemAttributeRelationMapper {

  /**
   * 通过id查询单条道具属性关系数据
   *
   * @param id 主键
   * @return 可选的道具属性关系
   */
  Optional<ItemAttributeRelation> selectById(Long id);

  /**
   * 新增道具属性关系
   *
   * @param itemAttributeRelation 实例对象
   * @return 影响行数
   */
  int insert(ItemAttributeRelation itemAttributeRelation);

  /**
   * 修改道具属性关系
   *
   * @param itemAttributeRelation 实例对象
   * @return 影响行数
   */
  int updateById(ItemAttributeRelation itemAttributeRelation);

  /**
   * 通过id删除道具属性关系
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

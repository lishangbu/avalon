package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.ItemPocket;
import java.util.Optional;

/**
 * 道具口袋(item_pocket)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface ItemPocketMapper {

  /**
   * 通过id查询单条道具口袋数据
   *
   * @param id 主键
   * @return 可选的道具口袋
   */
  Optional<ItemPocket> selectById(Long id);

  /**
   * 新增道具口袋
   *
   * @param itemPocket 实例对象
   * @return 影响行数
   */
  int insert(ItemPocket itemPocket);

  /**
   * 修改道具口袋
   *
   * @param itemPocket 实例对象
   * @return 影响行数
   */
  int updateById(ItemPocket itemPocket);

  /**
   * 通过id删除道具口袋
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

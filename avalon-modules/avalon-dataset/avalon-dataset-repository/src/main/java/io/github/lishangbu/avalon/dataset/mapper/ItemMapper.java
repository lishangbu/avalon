package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.Item;
import java.util.Optional;

/**
 * 道具(item)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface ItemMapper {

  /**
   * 通过id查询单条道具数据
   *
   * @param id 主键
   * @return 可选的道具
   */
  Optional<Item> selectById(Long id);

  /**
   * 新增道具
   *
   * @param item 实例对象
   * @return 影响行数
   */
  int insert(Item item);

  /**
   * 修改道具
   *
   * @param item 实例对象
   * @return 影响行数
   */
  int updateById(Item item);

  /**
   * 通过id删除道具
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

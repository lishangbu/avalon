package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.ItemFlingEffect;
import java.util.Optional;

/**
 * 道具"投掷"效果(item_fling_effect)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface ItemFlingEffectMapper {

  /**
   * 通过id查询单条道具"投掷"效果数据
   *
   * @param id 主键
   * @return 可选的道具"投掷"效果
   */
  Optional<ItemFlingEffect> selectById(Long id);

  /**
   * 新增道具"投掷"效果
   *
   * @param itemFlingEffect 实例对象
   * @return 影响行数
   */
  int insert(ItemFlingEffect itemFlingEffect);

  /**
   * 修改道具"投掷"效果
   *
   * @param itemFlingEffect 实例对象
   * @return 影响行数
   */
  int updateById(ItemFlingEffect itemFlingEffect);

  /**
   * 通过id删除道具"投掷"效果
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

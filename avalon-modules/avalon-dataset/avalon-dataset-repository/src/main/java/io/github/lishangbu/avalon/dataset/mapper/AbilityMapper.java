package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.Ability;
import java.util.Optional;

/**
 * 特性(ability)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface AbilityMapper {

  /**
   * 通过id查询单条特性数据
   *
   * @param id 主键
   * @return 可选的特性
   */
  Optional<Ability> selectById(Long id);

  /**
   * 新增特性
   *
   * @param ability 实例对象
   * @return 影响行数
   */
  int insert(Ability ability);

  /**
   * 修改特性
   *
   * @param ability 实例对象
   * @return 影响行数
   */
  int updateById(Ability ability);

  /**
   * 通过id删除特性
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass;
import java.util.Optional;

/**
 * 招式伤害类别(move_damage_class)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface MoveDamageClassMapper {

  /**
   * 通过id查询单条招式伤害类别数据
   *
   * @param id 主键
   * @return 可选的招式伤害类别
   */
  Optional<MoveDamageClass> selectById(Long id);

  /**
   * 新增招式伤害类别
   *
   * @param moveDamageClass 实例对象
   * @return 影响行数
   */
  int insert(MoveDamageClass moveDamageClass);

  /**
   * 修改招式伤害类别
   *
   * @param moveDamageClass 实例对象
   * @return 影响行数
   */
  int updateById(MoveDamageClass moveDamageClass);

  /**
   * 通过id删除招式伤害类别
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

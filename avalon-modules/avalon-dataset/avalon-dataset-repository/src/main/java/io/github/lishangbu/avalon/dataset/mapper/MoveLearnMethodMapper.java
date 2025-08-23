package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.MoveLearnMethod;
import java.util.Optional;

/**
 * 学习招式的方法(move_learn_method)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface MoveLearnMethodMapper {

  /**
   * 通过id查询单条学习招式的方法数据
   *
   * @param id 主键
   * @return 可选的学习招式的方法
   */
  Optional<MoveLearnMethod> selectById(Long id);

  /**
   * 新增学习招式的方法
   *
   * @param moveLearnMethod 实例对象
   * @return 影响行数
   */
  int insert(MoveLearnMethod moveLearnMethod);

  /**
   * 修改学习招式的方法
   *
   * @param moveLearnMethod 实例对象
   * @return 影响行数
   */
  int updateById(MoveLearnMethod moveLearnMethod);

  /**
   * 通过id删除学习招式的方法
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

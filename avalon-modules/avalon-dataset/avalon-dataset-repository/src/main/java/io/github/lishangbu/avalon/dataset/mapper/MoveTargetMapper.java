package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.MoveTarget;
import java.util.Optional;

/**
 * 招式指向目标(move_target)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface MoveTargetMapper {

  /**
   * 通过id查询单条招式指向目标数据
   *
   * @param id 主键
   * @return 可选的招式指向目标
   */
  Optional<MoveTarget> selectById(Long id);

  /**
   * 新增招式指向目标
   *
   * @param moveTarget 实例对象
   * @return 影响行数
   */
  int insert(MoveTarget moveTarget);

  /**
   * 修改招式指向目标
   *
   * @param moveTarget 实例对象
   * @return 影响行数
   */
  int updateById(MoveTarget moveTarget);

  /**
   * 通过id删除招式指向目标
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

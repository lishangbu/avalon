package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.MoveAilment;
import java.util.Optional;

/**
 * 招式导致的状态异常(move_ailment)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface MoveAilmentMapper {

  /**
   * 通过id查询单条招式导致的状态异常数据
   *
   * @param id 主键
   * @return 可选的招式导致的状态异常
   */
  Optional<MoveAilment> selectById(Long id);

  /**
   * 新增招式导致的状态异常
   *
   * @param moveAilment 实例对象
   * @return 影响行数
   */
  int insert(MoveAilment moveAilment);

  /**
   * 修改招式导致的状态异常
   *
   * @param moveAilment 实例对象
   * @return 影响行数
   */
  int updateById(MoveAilment moveAilment);

  /**
   * 通过id删除招式导致的状态异常
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

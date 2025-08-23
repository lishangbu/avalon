package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.Move;
import java.util.Optional;

/**
 * 招式(move)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface MoveMapper {

  /**
   * 通过id查询单条招式数据
   *
   * @param id 主键
   * @return 可选的招式
   */
  Optional<Move> selectById(Long id);

  /**
   * 新增招式
   *
   * @param move 实例对象
   * @return 影响行数
   */
  int insert(Move move);

  /**
   * 修改招式
   *
   * @param move 实例对象
   * @return 影响行数
   */
  int updateById(Move move);

  /**
   * 通过id删除招式
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

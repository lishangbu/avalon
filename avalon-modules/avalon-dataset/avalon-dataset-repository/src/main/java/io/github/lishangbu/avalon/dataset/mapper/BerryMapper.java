package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import java.util.Optional;

/**
 * 树果(berry)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface BerryMapper {

  /**
   * 通过id查询单条树果数据
   *
   * @param id 主键
   * @return 可选的树果
   */
  Optional<Berry> selectById(Long id);

  /**
   * 新增树果
   *
   * @param berry 实例对象
   * @return 影响行数
   */
  int insert(Berry berry);

  /**
   * 修改树果
   *
   * @param berry 实例对象
   * @return 影响行数
   */
  int updateById(Berry berry);

  /**
   * 通过id删除树果
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

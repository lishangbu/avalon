package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.MoveCategory;
import java.util.Optional;

/**
 * 招式分类(move_category)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface MoveCategoryMapper {

  /**
   * 通过id查询单条招式分类数据
   *
   * @param id 主键
   * @return 可选的招式分类
   */
  Optional<MoveCategory> selectById(Long id);

  /**
   * 新增招式分类
   *
   * @param moveCategory 实例对象
   * @return 影响行数
   */
  int insert(MoveCategory moveCategory);

  /**
   * 修改招式分类
   *
   * @param moveCategory 实例对象
   * @return 影响行数
   */
  int updateById(MoveCategory moveCategory);

  /**
   * 通过id删除招式分类
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

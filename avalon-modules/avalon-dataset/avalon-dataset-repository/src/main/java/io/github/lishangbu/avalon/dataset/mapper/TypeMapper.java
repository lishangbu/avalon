package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.Type;
import java.util.List;
import java.util.Optional;

/**
 * 属性(type)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface TypeMapper {

  /**
   * 通过id查询单条属性数据
   *
   * @param id 主键
   * @return 可选的属性
   */
  Optional<Type> selectById(Long id);

  /**
   * 新增属性
   *
   * @param type 实例对象
   * @return 影响行数
   */
  int insert(Type type);

  /**
   * 修改属性
   *
   * @param type 实例对象
   * @return 影响行数
   */
  int updateById(Type type);

  /**
   * 通过id删除属性
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);

  /**
   * 根据条件查询符合条件的数据
   *
   * @param type 属性
   * @return 符合条件的属性列表
   */
  List<Type> selectAll(Type type);
}

package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.EggGroup;
import java.util.Optional;

/**
 * 蛋组(egg_group)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface EggGroupMapper {

  /**
   * 通过id查询单条蛋组数据
   *
   * @param id 主键
   * @return 可选的蛋组
   */
  Optional<EggGroup> selectById(Long id);

  /**
   * 新增蛋组
   *
   * @param eggGroup 实例对象
   * @return 影响行数
   */
  int insert(EggGroup eggGroup);

  /**
   * 修改蛋组
   *
   * @param eggGroup 实例对象
   * @return 影响行数
   */
  int updateById(EggGroup eggGroup);

  /**
   * 通过id删除蛋组
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

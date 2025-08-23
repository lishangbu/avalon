package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.Machine;
import java.util.Optional;

/**
 * 技能学习机器(machine)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface MachineMapper {

  /**
   * 通过id查询单条技能学习机器数据
   *
   * @param id 主键
   * @return 可选的技能学习机器
   */
  Optional<Machine> selectById(Long id);

  /**
   * 新增技能学习机器
   *
   * @param machine 实例对象
   * @return 影响行数
   */
  int insert(Machine machine);

  /**
   * 修改技能学习机器
   *
   * @param machine 实例对象
   * @return 影响行数
   */
  int updateById(Machine machine);

  /**
   * 通过id删除技能学习机器
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

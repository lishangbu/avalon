package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.Role;
import java.util.Optional;

/**
 * 角色信息(role)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface RoleMapper {

  /**
   * 通过id查询单条角色信息数据
   *
   * @param id 主键
   * @return 可选的角色信息
   */
  Optional<Role> selectById(Long id);

  /**
   * 统计角色信息总行数
   *
   * @param role 查询条件
   * @return 总行数
   */
  long count(Role role);

  /**
   * 新增角色信息
   *
   * @param role 实例对象
   * @return 影响行数
   */
  int insert(Role role);

  /**
   * 修改角色信息
   *
   * @param role 实例对象
   * @return 影响行数
   */
  int updateById(Role role);

  /**
   * 通过id删除角色信息
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}

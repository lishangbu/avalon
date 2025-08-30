package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.Role;
import java.util.List;
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

  /**
   * 根据查询条件查询角色列表
   *
   * @param role 角色查询条件
   * @return 返回的角色列表
   */
  List<Role> selectAll(Role role);
}

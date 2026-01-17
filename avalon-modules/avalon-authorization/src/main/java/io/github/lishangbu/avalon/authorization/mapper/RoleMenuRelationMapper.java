package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.RoleMenuRelation;
import org.apache.ibatis.annotations.Param;

/// 角色与菜单关联的 Mapper 接口
///
/// 提供对角色-菜单中间表的插入与删除操作
///
/// @author lishangbu
/// @since 2025/12/12
public interface RoleMenuRelationMapper {

  /// 插入一条角色与菜单的关联记录
  ///
  /// @param relation 包含 roleId 与 menuId 的关联实体
  /// @return 受影响的行数
  int insert(RoleMenuRelation relation);

  /// 根据角色 ID 和菜单 ID 删除关联关系
  ///
  /// @param roleId 角色 ID
  /// @param menuId 菜单 ID
  /// @return 受影响的行数
  int deleteByRoleIdAndMenuId(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

  /// 根据角色 ID 删除所有关联的菜单关系
  ///
  /// @param roleId 角色 ID
  /// @return 受影响的行数
  int deleteByRoleId(@Param("roleId") Long roleId);
}

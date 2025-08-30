package io.github.lishangbu.avalon.authorization.service;

import io.github.lishangbu.avalon.authorization.entity.Permission;
import io.github.lishangbu.avalon.authorization.model.PermissionTreeNode;
import java.util.List;

/**
 * 权限服务接口
 *
 * @author lishangbu
 * @since 2025/8/28
 */
public interface PermissionService {
  /**
   * 根据角色代码获取权限树
   *
   * @param roleCodes 角色代码
   * @return 权限树节点列表
   */
  List<PermissionTreeNode> listPermissionTreeByRoleCodes(List<String> roleCodes);

  /**
   * 根据查询条件获取所有权限树节点
   *
   * @param permission 查询条件
   * @return 权限树节点列表
   */
  List<PermissionTreeNode> listPermissionTreeNodes(Permission permission);

  /**
   * 根据查询条件获取所有权限
   *
   * @param permission 查询条件
   * @return 权限列表
   */
  List<Permission> listPermissions(Permission permission);
}

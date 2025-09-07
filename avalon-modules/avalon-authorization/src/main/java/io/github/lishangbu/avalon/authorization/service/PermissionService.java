package io.github.lishangbu.avalon.authorization.service;

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
}

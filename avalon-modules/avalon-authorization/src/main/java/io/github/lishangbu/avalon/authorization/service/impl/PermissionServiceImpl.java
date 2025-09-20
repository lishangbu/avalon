package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.entity.Permission;
import io.github.lishangbu.avalon.authorization.model.PermissionTreeNode;
import io.github.lishangbu.avalon.authorization.repository.PermissionRepository;
import io.github.lishangbu.avalon.authorization.service.PermissionService;
import io.github.lishangbu.avalon.web.util.TreeUtils;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 权限服务实现
 *
 * <p>优化说明：抽取重复的树构建逻辑到私有方法，增加空集合处理，统一日志输出，提升可读性与健壮性
 *
 * @author lishangbu
 * @since 2025/8/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
  private final PermissionRepository permissionMapper;

  @Override
  public List<PermissionTreeNode> listPermissionTreeByRoleCodes(List<String> roleCodes) {
    // 根据角色编码查询权限列表
    List<Permission> permissions = permissionMapper.findAllEnabledPermissionsByRoleCodes(roleCodes);
    log.debug("根据角色编码获取到 [{}] 条权限记录", permissions == null ? 0 : permissions.size());
    return buildTreeFromPermissions(permissions);
  }

  /**
   * 将权限实体列表转换为树节点并构建树结构 1) 处理空集合，返回不可变空列表 2) 将 Permission 映射为 PermissionTreeNode 3) 使用通用的 TreeUtils
   * 构建树
   *
   * @param permissions 权限实体列表，允许为 null
   * @return 树结构的 PermissionTreeNode 列表，永远不返回 null
   */
  private List<PermissionTreeNode> buildTreeFromPermissions(List<Permission> permissions) {
    if (CollectionUtils.isEmpty(permissions)) {
      return Collections.emptyList();
    }

    List<PermissionTreeNode> treeNodes =
        permissions.stream().map(PermissionTreeNode::new).collect(Collectors.toList());

    return TreeUtils.buildTree(
        treeNodes,
        PermissionTreeNode::getId,
        PermissionTreeNode::getParentId,
        PermissionTreeNode::setChildren);
  }
}

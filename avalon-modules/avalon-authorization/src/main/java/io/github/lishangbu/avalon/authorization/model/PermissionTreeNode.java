package io.github.lishangbu.avalon.authorization.model;

import io.github.lishangbu.avalon.authorization.entity.Permission;
import java.util.List;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/**
 * 权限树节点
 *
 * @author lishangbu
 * @since 2025/8/28
 */
@Data
public class PermissionTreeNode extends Permission {
  private List<PermissionTreeNode> children;

  public PermissionTreeNode(Permission permission) {
    if (permission != null) {
      BeanUtils.copyProperties(permission, this);
    }
  }
}

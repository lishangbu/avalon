package io.github.lishangbu.avalon.auth.entity;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户角色关系
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Table
public class UserRoleRelation implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private Long userId;

  private Long roleId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getRoleId() {
    return roleId;
  }

  public void setRoleId(Long roleId) {
    this.roleId = roleId;
  }
}

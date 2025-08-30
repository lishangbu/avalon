package io.github.lishangbu.avalon.authorization.model;

import io.github.lishangbu.avalon.authorization.entity.Profile;
import io.github.lishangbu.avalon.authorization.entity.Role;
import java.util.List;
import lombok.Data;

/**
 * 用户详情
 *
 * <p>包含用户的基本信息、角色信息以及个人资料。
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@Data
public class UserDetail {
  /** 用户ID */
  private Long id;

  /** 用户名 */
  private String username;

  /** 用户个人资料 */
  private Profile profile;

  /** 用户角色列表 */
  private List<Role> roles;
}

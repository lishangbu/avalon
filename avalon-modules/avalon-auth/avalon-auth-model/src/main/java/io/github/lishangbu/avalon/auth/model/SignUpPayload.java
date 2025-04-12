package io.github.lishangbu.avalon.auth.model;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 注册对象
 *
 * @author lishangbu
 * @since 2025/4/9
 */
@Data
public class SignUpPayload {
  /** 注册用的用户名 */
  @NotEmpty(message = "请输入用户名")
  private String username;

  /** 注册用的密码 */
  @NotEmpty(message = "请输入密码")
  private String password;

  /** 角色代码 */
  private String roleCode;
}

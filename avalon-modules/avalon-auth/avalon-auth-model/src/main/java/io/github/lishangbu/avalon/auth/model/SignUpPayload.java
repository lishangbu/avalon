package io.github.lishangbu.avalon.auth.model;

import jakarta.validation.constraints.NotEmpty;

/**
 * 注册对象
 *
 * @author lishangbu
 * @since 2025/4/9
 */
public class SignUpPayload {
  /** 注册用的用户名 */
  @NotEmpty(message = "请输入用户名")
  private String username;

  /** 注册用的密码 */
  @NotEmpty(message = "请输入密码")
  private String password;

  /** 角色代码 */
  private String roleCode;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getRoleCode() {
    return roleCode;
  }

  public void setRoleCode(String roleCode) {
    this.roleCode = roleCode;
  }
}

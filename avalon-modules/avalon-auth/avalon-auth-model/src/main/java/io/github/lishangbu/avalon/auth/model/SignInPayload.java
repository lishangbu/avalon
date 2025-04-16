package io.github.lishangbu.avalon.auth.model;

import jakarta.validation.constraints.NotEmpty;

/**
 * 登陆对象
 *
 * @author lishangbu
 * @since 2025/4/9
 */
public class SignInPayload {
  /** 用户名 */
  @NotEmpty(message = "请输入用户名")
  private String username;

  /** 密码 */
  @NotEmpty(message = "请输入密码")
  private String password;

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
}

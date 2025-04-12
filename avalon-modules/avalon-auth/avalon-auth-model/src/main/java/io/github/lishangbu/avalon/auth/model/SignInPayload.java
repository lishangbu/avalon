package io.github.lishangbu.avalon.auth.model;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 登陆对象
 *
 * @author lishangbu
 * @since 2025/4/9
 */
@Data
public class SignInPayload {
  /** 用户名 */
  @NotEmpty(message = "请输入用户名")
  private String username;

  /** 密码 */
  @NotEmpty(message = "请输入密码")
  private String password;
}

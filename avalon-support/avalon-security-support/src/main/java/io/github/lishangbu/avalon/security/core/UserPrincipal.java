package io.github.lishangbu.avalon.security.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 用户
 *
 * @author lishangbu
 * @since 2025/4/8
 */
public record UserPrincipal(
    Long id,
    String username,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password,
    Collection<? extends GrantedAuthority> authorities)
    implements UserDetails {

  /**
   * 获取用户的权限
   *
   * @return 用户的权限集合
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  /**
   * 获取用户的密码
   *
   * @return 用户的密码
   */
  @Override
  public String getPassword() {
    return password;
  }

  /**
   * 获取用户的用户名
   *
   * @return 用户名
   */
  @Override
  public String getUsername() {
    return username;
  }
}

package io.github.lishangbu.avalon.security.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 用户
 *
 * @author lishangbu
 * @since 2025/4/8
 */
@RequiredArgsConstructor
@ToString
public class UserPrincipal implements UserDetails {
  @Getter private final Long id;

  private final String username;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private final String password;

  private final Collection<? extends GrantedAuthority> authorities;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return this.authorities;
  }

  @Override
  public String getPassword() {
    return this.password;
  }

  @Override
  public String getUsername() {
    return this.username;
  }
}

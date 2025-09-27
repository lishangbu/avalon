package io.github.lishangbu.avalon.oauth2.common.userdetails;

import java.io.Serial;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

/**
 * 用户信息
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Getter
@Setter
public class UserInfo extends User implements OAuth2AuthenticatedPrincipal {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 附加参数：用于在获取 Token 接口返回 */
  @Getter private final Map<String, Object> additionalParameters = new HashMap<>();

  public UserInfo(
      String username,
      @Nullable String password,
      Collection<? extends GrantedAuthority> authorities) {
    super(username, password, authorities);
  }

  public UserInfo(
      String username,
      @Nullable String password,
      boolean enabled,
      boolean accountNonExpired,
      boolean credentialsNonExpired,
      boolean accountNonLocked,
      Collection<? extends GrantedAuthority> authorities) {
    super(
        username,
        password,
        enabled,
        accountNonExpired,
        credentialsNonExpired,
        accountNonLocked,
        authorities);
  }

  @Override
  public Map<String, Object> getAttributes() {
    return additionalParameters;
  }

  @Override
  @NullMarked
  public String getName() {
    return super.getUsername();
  }

  @Override
  @NullMarked
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName()).append(" [");
    sb.append("Username=").append(getUsername()).append(", ");
    sb.append("Password=[PROTECTED], ");
    sb.append("Enabled=").append(isEnabled()).append(", ");
    sb.append("AdditionalParameters").append(additionalParameters);
    sb.append("AccountNonExpired=").append(isAccountNonExpired()).append(", ");
    sb.append("CredentialsNonExpired=").append(isCredentialsNonExpired()).append(", ");
    sb.append("AccountNonLocked=").append(isAccountNonLocked()).append(", ");
    sb.append("Granted Authorities=").append(getAuthorities()).append("]");
    return sb.toString();
  }
}

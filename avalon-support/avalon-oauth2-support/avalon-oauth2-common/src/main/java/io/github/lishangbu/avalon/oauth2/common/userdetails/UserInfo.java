package io.github.lishangbu.avalon.oauth2.common.userdetails;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serial;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

/**
 * 用户信息
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class UserInfo implements UserDetails, CredentialsContainer, OAuth2AuthenticatedPrincipal {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 用户标识 */
  private String id;

  /** 用户名 */
  private String username;

  /** 密码 */
  private String password;

  /** 角色代码 */
  private String roleCodes;

  private final Map<String, Object> attributes;

  /**
   * 构造函数
   *
   * @param id 用户标识
   * @param username 用户名
   * @param password 密码
   * @param roleCodes 角色编码
   */
  public UserInfo(String id, String username, String password, String roleCodes) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.roleCodes = roleCodes;
    this.attributes = new HashMap<>();
  }

  /**
   * 构造函数
   *
   * @param id 用户标识
   * @param username 用户名
   * @param password 密码
   * @param roleCodes 角色编码
   * @param attributes 属性
   */
  public UserInfo(
      String id,
      String username,
      String password,
      String roleCodes,
      Map<String, Object> attributes) {
    if (attributes == null) {
      this.attributes = new HashMap<>();
    } else {
      this.attributes = attributes;
    }
    this.id = id;
    this.username = username;
    this.password = password;
    this.roleCodes = roleCodes;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return this.attributes;
  }

  /**
   * 获取角色权限
   *
   * @return 用户权限集合
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // 转换 roleCodes 为权限集合
    return AuthorityUtils.commaSeparatedStringToAuthorityList(this.roleCodes);
  }

  /**
   * 获取密码
   *
   * @return 密码
   */
  @Override
  public String getPassword() {
    return this.password;
  }

  /**
   * 获取用户名
   *
   * @return 用户名
   */
  @Override
  public String getUsername() {
    return this.username;
  }

  /** 清空密码 */
  @Override
  public void eraseCredentials() {
    this.password = null;
  }

  @Override
  public String getName() {
    return this.username;
  }
}

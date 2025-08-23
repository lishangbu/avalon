package io.github.lishangbu.avalon.oauth2.common.userdetails;

import com.fasterxml.jackson.annotation.*;
import java.util.Map;

/**
 * 用户信息的 Jackson Mixin 类，用于序列化和反序列化。
 *
 * @author lishangbu
 * @since 2025/8/22
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(
    value = {
      "authorities",
      "enabled",
      "accountNonLocked",
      "accountNonExpired",
      "credentialsNonExpired",
      "name"
    })
public abstract class UserInfoMixin {
  /**
   * Mixin Constructor.
   *
   * @param id 用户标识
   * @param username 用户名
   * @param password 密码
   * @param roleCodes 角色编码
   */
  @JsonCreator
  public UserInfoMixin(
      @JsonProperty("id") String id,
      @JsonProperty("username") String username,
      @JsonProperty("password") String password,
      @JsonProperty("roleCodes") String roleCodes,
      @JsonProperty("attributes") Map<String, Object> attributes) {
    // No implementation needed, this is just a mixin for Jackson

  }
}

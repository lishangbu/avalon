package io.github.lishangbu.avalon.authorization.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import lombok.ToString;

/**
 * 用户信息(User)实体类
 *
 * @author lishangbu
 * @since 2025/08/19
 */
@Data
public class User implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  private Long id;

  /** 用户名 */
  private String username;

  /** 密码 */
  @ToString.Exclude
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String password;
}

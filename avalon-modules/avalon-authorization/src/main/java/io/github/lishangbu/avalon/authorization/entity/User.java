package io.github.lishangbu.avalon.authorization.entity;

import io.github.lishangbu.avalon.mybatis.id.Id;
import io.github.lishangbu.avalon.mybatis.id.IdType;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.security.core.SpringSecurityCoreVersion;

/**
 * 用户信息(User)实体类
 *
 * @author lishangbu
 * @since 2025/08/19
 */
@Data
public class User implements Serializable {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 主键 */
  @Id(type = IdType.FLEX)
  private Long id;

  /** 用户名 */
  private String username;

  /** 密码 */
  private String password;
}

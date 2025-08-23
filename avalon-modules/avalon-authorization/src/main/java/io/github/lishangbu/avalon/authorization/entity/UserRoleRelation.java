package io.github.lishangbu.avalon.authorization.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.security.core.SpringSecurityCoreVersion;

/**
 * 用户角色关系(UserRoleRelation)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class UserRoleRelation implements Serializable {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 用户ID */
  private Long userId;

  /** 角色ID */
  private Long roleId;
}

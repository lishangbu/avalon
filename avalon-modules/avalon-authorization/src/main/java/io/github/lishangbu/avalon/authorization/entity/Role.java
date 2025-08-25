package io.github.lishangbu.avalon.authorization.entity;

import io.github.lishangbu.avalon.mybatis.id.Id;
import io.github.lishangbu.avalon.mybatis.id.IdType;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.security.core.SpringSecurityCoreVersion;

/**
 * 角色信息(Role)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class Role implements Serializable {
  @Serial private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

  /** 主键 */
  @Id(type = IdType.FLEX)
  private Long id;

  /** 角色代码 */
  private String code;

  /** 角色名称 */
  private String name;

  /** 角色是否启用 */
  private Boolean enabled;
}

package io.github.lishangbu.avalon.authorization.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Sequence;

/**
 * 角色信息(Role)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class Role implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  @Sequence("role_id_seq")
  private Integer id;

  /** 角色代码 */
  private String code;

  /** 角色名称 */
  private String name;

  /** 角色是否启用 */
  private Boolean enabled;

  /** 与菜单的中间表集合，Spring Data JDBC 会加载中间表行，但不会自动加载 Menu 实体 */
  @MappedCollection(idColumn = "role_id")
  private Set<RoleMenuRelation> roleMenus;
}

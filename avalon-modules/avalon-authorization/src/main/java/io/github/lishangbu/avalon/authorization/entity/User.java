package io.github.lishangbu.avalon.authorization.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户信息(User)实体类
 *
 * @author lishangbu
 * @since 2025/08/19
 */
@Data
@Table
public class User implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id private Long id;

  /** 用户名 */
  private String username;

  /** 密码 */
  @ToString.Exclude
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String password;

  /** 与角色的中间表集合，Spring Data JDBC 会加载中间表行，但不会自动加载 Role 实体 */
  @MappedCollection(idColumn = "user_id")
  private Set<UserRoleRelation> userRoles;
}

package io.github.lishangbu.avalon.authorization.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import lombok.Data;
import lombok.ToString;

/// 用户信息(User)实体类
///
/// 表示系统中的用户基本信息，密码为写入-only 且不会被打印
///
/// @author lishangbu
/// @since 2025/08/19
@Data
@Entity
@Table(name = "users", comment = "用户表，存储系统中的用户基本信息")
public class User implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Flex
  @Id
  @Column(comment = "主键")
  private Long id;

  /// 用户名
  @Column(comment = "用户名", length = 20)
  private String username;

  /// 密码（写入-only，不会在 toString 中显示）
  @ToString.Exclude
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @Column(comment = "密码", length = 100)
  private String hashedPassword;

  /// 用户与角色多对多关系
  @ManyToMany
  @JoinTable(
      name = "user_role_relation",
      comment = "用户与角色的关联表",
      joinColumns = @JoinColumn(name = "user_id", comment = "用户ID"),
      inverseJoinColumns = @JoinColumn(name = "role_id", comment = "角色ID"))
  @ToString.Exclude
  private Set<Role> roles;
}

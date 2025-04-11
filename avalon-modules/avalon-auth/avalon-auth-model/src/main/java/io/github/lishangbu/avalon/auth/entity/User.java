package io.github.lishangbu.avalon.auth.entity;

import io.github.lishangbu.avalon.orm.id.annotation.FlexIdGenerator;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 用户信息
 *
 * @author lishangbu
 * @since 2025/3/30
 */
@Data
@DynamicInsert
@DynamicUpdate
@Entity
@Table(
    name = "[user]",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_USER_USERNAME",
          columnNames = {"username"})
    })
public class User {

  @Id
  @FlexIdGenerator
  @Comment("主键")
  private Long id;

  /** 用户名 */
  @Comment("用户名")
  @Column(nullable = false, length = 20)
  private String username;

  @Comment("密码")
  @Column(nullable = false, length = 200)
  private String password;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "user_role",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles;
}

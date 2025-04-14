package io.github.lishangbu.avalon.auth.entity;

import io.github.lishangbu.avalon.orm.id.annotation.FlexIdGenerator;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
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
    name = "[USER]",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_USER_USERNAME",
          columnNames = {"USERNAME"})
    })
@Comment("用户")
public class User implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

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
      name = "USER_ROLE",
      joinColumns = @JoinColumn(name = "USER_ID"),
      inverseJoinColumns = @JoinColumn(name = "ROLE_ID"))
  private Set<Role> roles;
}

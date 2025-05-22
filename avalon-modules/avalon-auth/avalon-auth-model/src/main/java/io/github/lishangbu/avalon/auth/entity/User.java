package io.github.lishangbu.avalon.auth.entity;

import io.github.lishangbu.avalon.orm.id.annotation.FlexIdGenerator;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 用户信息
 *
 * @author lishangbu
 * @since 2025/3/30
 */
@DynamicInsert
@DynamicUpdate
@Entity
@Table(
    name = "[user]",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_user_username",
          columnNames = {"username"})
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

  /**
   * 使用JoinTable来描述中间表，并描述中间表中外键与User,Role的映射关系 joinColumns用来描述User与中间表中的映射关系
   * inverseJoinColums用来描述Role与中间表中的映射关系
   */
  @JoinTable(
      name = "user_role_relation",
      joinColumns =
          @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_user_id")),
      inverseJoinColumns =
          @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_user_role_id")))
  @ManyToMany
  private List<Role> roles;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public List<Role> getRoles() {
    return roles;
  }

  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }
}

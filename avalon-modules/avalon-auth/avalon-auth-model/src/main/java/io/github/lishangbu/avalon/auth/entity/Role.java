package io.github.lishangbu.avalon.auth.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.Comment;

@Entity
@Comment("角色")
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_role_code",
          columnNames = {"code"})
    })
public class Role implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "role_seq_gen")
  @TableGenerator(name = "role_seq_gen", table = "hibernate_sequences", pkColumnValue = "role")
  @Comment("主键")
  private Integer id;

  @Column(nullable = false, length = 20)
  @Comment("角色代码")
  private String code;

  /** 让user维护外键表 */
  @ManyToMany(mappedBy = "roles")
  private List<User> users;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public List<User> getUsers() {
    return users;
  }

  public void setUsers(List<User> users) {
    this.users = users;
  }
}

package io.github.lishangbu.avalon.auth.entity;

import io.github.lishangbu.avalon.orm.id.annotation.FlexIdGenerator;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
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
  @FlexIdGenerator
  @Comment("主键")
  private Long id;

  @Column(nullable = false, length = 20)
  @Comment("角色代码")
  private String code;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}

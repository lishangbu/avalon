package io.github.lishangbu.avalon.auth.entity;

import io.github.lishangbu.avalon.orm.id.annotation.FlexIdGenerator;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Entity
@Data
@Comment("角色")
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_ROLE_CODE",
          columnNames = {"CODE"})
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
}

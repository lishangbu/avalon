package io.github.lishangbu.avalon.auth.entity;

import io.github.lishangbu.avalon.orm.id.annotation.FlexIdGenerator;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Entity
@Data
public class Role {
  @Id
  @FlexIdGenerator
  @Comment("主键")
  private Long id;

  @Column(nullable = false, length = 20)
  @Comment("角色代码")
  private String code;
}

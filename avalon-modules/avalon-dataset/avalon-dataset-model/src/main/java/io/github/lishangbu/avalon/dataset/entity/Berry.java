package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Comment;

/**
 * 树果
 *
 * @author lishangbu
 * @since 2025/5/21
 */
@Comment("树果")
@Entity
public class Berry {
  /** ID */
  @Id
  @Comment("主键")
  private Integer id;
}

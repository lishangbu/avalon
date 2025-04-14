package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.util.List;
import lombok.Data;
import org.hibernate.annotations.Comment;

/**
 * 招式分类
 *
 * @author lishangbu
 * @since 2025/4/15
 */
@Data
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_MOVE_CATEGORY_NAME",
          columnNames = {"NAME"})
    })
public class MoveCategory {

  /** 分类 */
  @Id
  @Comment("主键，分类")
  @Column(length = 10)
  private String category;

  @Comment("名称")
  @Column(length = 10, nullable = false)
  private String name;

  /**
   * 一种招式分类有多个招式
   *
   * @see Move
   * @see Move#getType()
   */
  @OneToMany(mappedBy = "category")
  private List<Move> moves;
}

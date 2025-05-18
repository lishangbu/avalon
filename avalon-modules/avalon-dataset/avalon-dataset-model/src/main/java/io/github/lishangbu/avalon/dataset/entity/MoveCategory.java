package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.Comment;

/**
 * 招式分类
 *
 * @author lishangbu
 * @since 2025/4/15
 */
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_MOVE_CATEGORY_NAME",
          columnNames = {"name"})
    })
public class MoveCategory implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** 分类 */
  @Id
  @Comment("主键，分类")
  @Column(length = 10)
  private String category;

  /** 属性说明 */
  @Comment("说明")
  @Column(nullable = false, length = 300)
  private String description;

  /**
   * 一种招式分类有多个招式
   *
   * @see Move
   * @see Move#getType()
   */
  @OneToMany(mappedBy = "category")
  private List<Move> moves;

  @Comment("名称")
  @Column(length = 10, nullable = false)
  private String name;

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Move> getMoves() {
    return moves;
  }

  public void setMoves(List<Move> moves) {
    this.moves = moves;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}

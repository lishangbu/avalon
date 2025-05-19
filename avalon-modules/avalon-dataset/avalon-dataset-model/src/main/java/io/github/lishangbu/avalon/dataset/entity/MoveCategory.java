package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;
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
          name = "uk_move_category_internal_name",
          columnNames = {"internal_name"})
    })
public class MoveCategory implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id
  @Comment("主键")
  private Integer id;

  /**
   * 内部名称
   *
   * <p>取百科中的分类数据
   */
  @Column(nullable = false, length = 10)
  @ColumnDefault("''")
  @Comment("内部名称")
  private String internalName;

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

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String internalName) {
    this.internalName = internalName;
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

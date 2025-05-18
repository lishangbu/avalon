package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.Comment;

/**
 * 属性
 *
 * @author lishangbu
 * @since 2025/4/14
 */
@Comment("属性")
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_type_name",
          columnNames = {"name"})
    })
public class Type implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 属性 */
  @Id
  @Comment("主键，属性")
  @Column(length = 20)
  private String type;

  /** 属性名称 */
  @Comment("属性名称")
  @Column(nullable = false, length = 30)
  private String name;

  /**
   * 一种属性有多个招式
   *
   * @see Move
   * @see Move#getType()
   */
  @OneToMany(mappedBy = "type")
  private List<Move> moves;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Move> getMoves() {
    return moves;
  }

  public void setMoves(List<Move> moves) {
    this.moves = moves;
  }
}

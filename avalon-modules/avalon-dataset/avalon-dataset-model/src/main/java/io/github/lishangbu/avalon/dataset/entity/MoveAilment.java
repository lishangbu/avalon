package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 招式导致的状态异常
 *
 * @author lishangbu
 * @since 2025/6/9
 */
@Comment("招式导致的状态异常")
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_move_ailment_internal_name",
          columnNames = {"internal_name"})
    })
public class MoveAilment implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id
  @Comment("主键")
  private Integer id;

  /** 内部名称 */
  @Column(nullable = false, length = 30)
  @ColumnDefault("''")
  @Comment("内部名称")
  private String internalName;

  /** 名称 */
  @Comment("名称")
  @Column(length = 30, nullable = false)
  private String name;

  /** 具有该状态异常的元数据 */
  @OneToMany(mappedBy = "ailment")
  private List<Move> moves;

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

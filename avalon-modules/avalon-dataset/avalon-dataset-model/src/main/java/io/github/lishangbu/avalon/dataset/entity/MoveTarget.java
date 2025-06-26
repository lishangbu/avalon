package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 战斗中招式可以指向的目标。目标可以是宝可梦、环境甚至其他招式
 *
 * @author lishangbu
 * @since 2025/6/9
 */
@Comment("招式指向目标")
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_move_target_internal_name",
          columnNames = {"internal_name"})
    })
public class MoveTarget implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id
  @Comment("主键")
  private Integer id;

  /** 内部名称 */
  @Column(nullable = false, length = 50)
  @ColumnDefault("''")
  @Comment("内部名称")
  private String internalName;

  /** 目标名称 */
  @Comment("名称")
  @Column(length = 50, nullable = false)
  private String name;

  /** 说明 */
  @Comment("说明")
  @Column(nullable = false, length = 300)
  @ColumnDefault("''")
  private String description;

  /** 具有该伤害类型的技能 */
  @OneToMany(mappedBy = "target")
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
}

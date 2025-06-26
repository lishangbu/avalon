package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 招式可以拥有的伤害类别，例如物理、特殊或非伤害性
 *
 * @author lishangbu
 * @since 2025/6/9
 */
@Entity
@Comment("招式可以拥有的伤害类别")
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_move_damage_class_internal_name",
          columnNames = {"internal_name"})
    })
public class MoveDamageClass implements Serializable {
  /** ID */
  @Id
  @Comment("主键")
  private Integer id;

  /** 名称 */
  @Comment("名称")
  private String name;

  /** 内部名称 */
  @Column(nullable = false, length = 100)
  @ColumnDefault("''")
  @Comment("内部名称")
  private String internalName;

  /** 描述 */
  @Comment("描述")
  @Column(length = 50, nullable = false)
  private String description;

  /** 具有该伤害类型的技能 */
  @OneToMany(mappedBy = "damageClass")
  private List<Move> moves;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
}

package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

/**
 * 属性关系
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@Comment("属性关系")
@Entity
public class TypeDamageRelation {

  /** ID */
  @Id
  @Comment("主键")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "type_damage_relation_seq_gen")
  @TableGenerator(
      name = "type_damage_relation_seq_gen",
      table = "hibernate_sequences",
      pkColumnValue = "type_damage_relation")
  private Integer id;

  @ManyToOne
  @JoinColumn(
      name = "attacker_type_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_attacker_type_id"))
  @Comment("属性")
  private Type attackerType;

  @ManyToOne
  @JoinColumn(
      name = "defender_type_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_defender_type_id"))
  @Comment("属性")
  private Type defenderType;

  private Float damageRate;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Type getAttackerType() {
    return attackerType;
  }

  public void setAttackerType(Type attacker) {
    this.attackerType = attacker;
  }

  public Type getDefenderType() {
    return defenderType;
  }

  public void setDefenderType(Type defender) {
    this.defenderType = defender;
  }

  public Float getDamageRate() {
    return damageRate;
  }

  public void setDamageRate(Float damageRate) {
    this.damageRate = damageRate;
  }
}

package io.github.lishangbu.avalon.dataset.entity;

import org.springframework.data.relational.core.mapping.Table;

/**
 * 属性关系
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@Table
public class TypeDamageRelation {

  /** 攻击者属性(内部名称) */
  private String attackerTypeInternalName;

  /** 防御者属性(内部名称) */
  private String defenderTypeInternalName;

  /** 伤害倍率 */
  private Float damageRate;

  public String getAttackerTypeInternalName() {
    return attackerTypeInternalName;
  }

  public void setAttackerTypeInternalName(String attackerTypeInternalName) {
    this.attackerTypeInternalName = attackerTypeInternalName;
  }

  public String getDefenderTypeInternalName() {
    return defenderTypeInternalName;
  }

  public void setDefenderTypeInternalName(String defenderTypeInternalName) {
    this.defenderTypeInternalName = defenderTypeInternalName;
  }

  public Float getDamageRate() {
    return damageRate;
  }

  public void setDamageRate(Float damageRate) {
    this.damageRate = damageRate;
  }
}

package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 属性伤害关系(TypeDamageRelation)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class TypeDamageRelation implements Serializable {
  @Serial private static final long serialVersionUID = 738084404669316218L;

  /** 攻击者属性(内部名称) */
  private String attackerTypeInternalName;

  /** 防御者属性(内部名称) */
  private String defenderTypeInternalName;

  /** 伤害倍率 */
  private Object damageRate;
}

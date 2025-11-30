package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 属性伤害关系(TypeDamageRelation)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class TypeDamageRelation implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id private Long id;

  /** 攻击者属性(内部名称) */
  private String attackerTypeInternalName;

  /** 防御者属性(内部名称) */
  private String defenderTypeInternalName;

  /** 伤害倍率 */
  private Double damageRate;
}

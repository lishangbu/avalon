package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 属性相互克制关系(TypeDamageRelation)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class TypeDamageRelation implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 攻击方ID */
  private Long attackingTypeId;

  /** 防御方ID */
  private Long defendingTypeId;

  /** 伤害倍数 */
  private Float multiplier;
}

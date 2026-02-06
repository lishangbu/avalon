package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 属性相互克制关系(TypeDamageRelation)实体类
///
/// 表示两种属性（type）之间的伤害倍数关系，用于计算属性相克结果
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class TypeDamageRelation implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 攻击方 ID
  @Id @Flex private Long attackingTypeId;

  /// 防御方 ID
  private Long defendingTypeId;

  /// 伤害倍数
  private Float multiplier;
}

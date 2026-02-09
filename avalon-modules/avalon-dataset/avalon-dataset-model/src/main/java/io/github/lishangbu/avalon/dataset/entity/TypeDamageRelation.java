package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/// 属性相互克制关系(TypeDamageRelation)实体类
///
/// 表示两种属性（type）之间的伤害倍数关系，用于计算属性相克结果
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@IdClass(TypeDamageRelation.TypeDamageRelationId.class)
@Table(comment = "属性相互克制关系")
public class TypeDamageRelation implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 攻击方 ID
  @Id
  @Column(comment = "攻击方 ID")
  private Long attackingTypeId;

  /// 防御方 ID
  @Id
  @Column(comment = "防御方 ID")
  private Long defendingTypeId;

  /// 伤害倍数
  @Column(comment = "伤害倍数")
  private Float multiplier;

  /// TypeDamageRelation 的复合主键类
  /// 包含 attackingTypeId 和 defendingTypeId
  @Getter
  @Setter
  public static class TypeDamageRelationId implements Serializable {
    private Long attackingTypeId;
    private Long defendingTypeId;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TypeDamageRelationId that = (TypeDamageRelationId) o;
      return attackingTypeId.equals(that.attackingTypeId)
          && defendingTypeId.equals(that.defendingTypeId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(attackingTypeId, defendingTypeId);
    }
  }
}

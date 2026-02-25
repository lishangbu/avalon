package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/// 属性相互克制关系(TypeDamageRelation)实体类
///
/// 表示两种属性（type）之间的伤害倍数关系，用于计算属性相克结果
///
/// @author lishangbu
/// @since 2025/08/20
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(comment = "属性相互克制关系")
public class TypeDamageRelation implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 复合主键
    @EmbeddedId private TypeDamageRelationId id;

    /// 伤害倍数
    @Column(comment = "伤害倍数")
    private Float multiplier;

    /// TypeDamageRelation 的复合主键类
    /// 包含 attackingType 和 defendingType
    @Getter
    @Setter
    @Embeddable
    public static class TypeDamageRelationId implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        /// 攻击方属性
        @ManyToOne
        @JoinColumn(name = "attacking_type_id", nullable = false, comment = "攻击方属性")
        private Type attackingType;

        /// 防御方属性
        @ManyToOne
        @JoinColumn(name = "defending_type_id", nullable = false, comment = "防御方属性")
        private Type defendingType;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TypeDamageRelationId that = (TypeDamageRelationId) o;
            return Objects.equals(getAttackingTypeId(), that.getAttackingTypeId())
                    && Objects.equals(getDefendingTypeId(), that.getDefendingTypeId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getAttackingTypeId(), getDefendingTypeId());
        }

        private Long getAttackingTypeId() {
            return attackingType != null ? attackingType.getId() : null;
        }

        private Long getDefendingTypeId() {
            return defendingType != null ? defendingType.getId() : null;
        }
    }
}

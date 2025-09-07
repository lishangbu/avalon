package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

/**
 * 属性伤害关系(TypeDamageRelation)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@IdClass(TypeDamageRelation.TypeDamageRelationId.class)
public class TypeDamageRelation implements Serializable {
  @Serial private static final long serialVersionUID = 738084404669316218L;

  /** 攻击者属性(内部名称) */
  @Id private String attackerTypeInternalName;

  /** 防御者属性(内部名称) */
  @Id private String defenderTypeInternalName;

  /** 伤害倍率 */
  private Object damageRate;

  public static class TypeDamageRelationId implements Serializable {
    private String attackerTypeInternalName;
    private String defenderTypeInternalName;

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

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TypeDamageRelationId that = (TypeDamageRelationId) o;
      return attackerTypeInternalName.equals(that.attackerTypeInternalName)
          && defenderTypeInternalName.equals(that.defenderTypeInternalName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(attackerTypeInternalName, defenderTypeInternalName);
    }
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    Class<?> oEffectiveClass =
        o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
    Class<?> thisEffectiveClass =
        this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) return false;
    TypeDamageRelation that = (TypeDamageRelation) o;
    return getAttackerTypeInternalName() != null
        && Objects.equals(getAttackerTypeInternalName(), that.getAttackerTypeInternalName())
        && getDefenderTypeInternalName() != null
        && Objects.equals(getDefenderTypeInternalName(), that.getDefenderTypeInternalName());
  }

  @Override
  public final int hashCode() {
    return Objects.hash(attackerTypeInternalName, defenderTypeInternalName);
  }
}

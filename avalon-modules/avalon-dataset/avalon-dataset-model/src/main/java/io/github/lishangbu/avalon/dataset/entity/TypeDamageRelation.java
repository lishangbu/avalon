package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 属性相互克制关系(TypeDamageRelation)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class TypeDamageRelation
    implements Serializable, Persistable<TypeDamageRelation.TypeDamageRelationId> {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id @Embedded.Empty private TypeDamageRelationId id;

  /** 类型 A 内部名称 */
  private Integer attackingTypeId;

  /** 类型 B 内部名称 */
  private Integer defendingTypeId;

  /** 伤害倍数 */
  private Float multiplier;

  @Override
  public boolean isNew() {
    return this.id == null;
  }

  @Getter
  @Setter
  public static class TypeDamageRelationId implements Serializable {
    private Integer attackingTypeId;
    private Integer defendingTypeId;

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

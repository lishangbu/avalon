package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 属性相互克制关系(TypeDamageRelation)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class TypeDamageRelation implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  @Sequence("type_damage_relation_id_seq")
  private Integer id;

  /** 类型 A 内部名称 */
  private String typeAInternalName;

  /** 类型 B 内部名称 */
  private String typeBInternalName;

  /** 伤害倍数 */
  private Double multiplier;
}

package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 招式伤害类别(MoveDamageClass)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class MoveDamageClass implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id private Long id;

  /** 招式伤害类别内部名称 */
  private String internalName;

  /** 招式伤害类别名称 */
  private String name;

  /** 招式伤害类别描述 */
  private String description;
}

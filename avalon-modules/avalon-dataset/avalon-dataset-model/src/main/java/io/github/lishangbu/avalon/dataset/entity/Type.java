package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 属性(Type)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class Type implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id private Long id;

  /** 属性内部名称 */
  private String internalName;

  /** 属性名称 */
  private String name;
}

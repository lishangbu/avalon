package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 招式导致的状态异常(MoveAilment)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Table
@Data
public class MoveAilment implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id private Long id;

  /** 招式导致的状态异常内部名称 */
  private String internalName;

  /** 招式导致的状态异常名称 */
  private String name;
}

package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 招式指向目标(MoveTarget)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class MoveTarget implements Serializable {
  @Serial private static final long serialVersionUID = 445025354746941748L;

  /** 主键 */
  private Long id;

  /** 招式指向目标内部名称 */
  private String internalName;

  /** 招式指向目标名称 */
  private String name;

  /** 招式指向目标描述 */
  private String description;
}

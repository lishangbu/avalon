package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 属性(Type)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class Type implements Serializable {
  @Serial private static final long serialVersionUID = -68547309302497308L;

  /** 主键 */
  private Long id;

  /** 属性内部名称 */
  private String internalName;

  /** 属性名称 */
  private String name;
}

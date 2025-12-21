package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 树果风味(BerryFlavor)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class BerryFlavor implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  private Long id;

  /** 内部名称 */
  private String internalName;

  /** 树果风味名称 */
  private String name;
}

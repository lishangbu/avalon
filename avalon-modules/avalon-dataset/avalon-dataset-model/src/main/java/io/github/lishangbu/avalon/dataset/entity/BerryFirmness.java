package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 树果硬度(BerryFirmness)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class BerryFirmness implements Serializable {
  @Serial private static final long serialVersionUID = 379274096201847612L;

  /** 主键 */
  private Long id;

  /** 内部名称 */
  private String internalName;

  /** 树果硬度名称 */
  private String name;
}

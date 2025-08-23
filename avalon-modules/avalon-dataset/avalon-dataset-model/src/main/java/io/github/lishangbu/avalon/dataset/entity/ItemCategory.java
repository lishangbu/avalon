package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 道具类别(ItemCategory)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class ItemCategory implements Serializable {
  @Serial private static final long serialVersionUID = 698222055482015322L;

  /** 主键 */
  private Long id;

  /** 内部名称 */
  private String internalName;

  /** 道具类别名称 */
  private String name;

  /** 该类别道具所属的口袋 */
  private String itemPocketInternalName;
}

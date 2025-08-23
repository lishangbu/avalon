package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 道具口袋(ItemPocket)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class ItemPocket implements Serializable {
  @Serial private static final long serialVersionUID = -63868359363578424L;

  /** 主键 */
  private Long id;

  /** 内部名称 */
  private String internalName;

  /** 道具口袋名称 */
  private String name;
}

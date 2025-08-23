package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 道具属性关系(ItemAttributeRelation)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class ItemAttributeRelation implements Serializable {
  @Serial private static final long serialVersionUID = -52048016504666011L;

  /** 主键 */
  private Long id;

  /** 道具ID */
  private Long itemId;

  /** 道具属性ID */
  private Long itemAttributeId;
}

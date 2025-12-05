package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 道具类别(ItemCategory)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class ItemCategory implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  @Sequence("item_category_id_seq")
  private Integer id;

  /** 内部名称 */
  private String internalName;

  /** 道具类别名称 */
  private String name;

  /** 该类别道具所属的口袋 */
  private String itemPocketInternalName;
}

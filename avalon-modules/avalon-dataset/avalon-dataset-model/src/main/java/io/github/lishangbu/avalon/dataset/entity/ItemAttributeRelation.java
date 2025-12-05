package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 道具属性关系(ItemAttributeRelation)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
@Table
public class ItemAttributeRelation implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  @Sequence("item_attribute_relation_id_seq")
  private Integer id;

  /** 道具ID */
  private Integer itemId;

  /** 道具属性ID */
  private Integer itemAttributeId;
}

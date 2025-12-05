package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 道具口袋(ItemPocket)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Table
@Data
public class ItemPocket implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  @Sequence("item_pocket_id_seq")
  private Integer id;

  /** 内部名称 */
  private String internalName;

  /** 道具口袋名称 */
  private String name;
}

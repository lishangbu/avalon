package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 道具属性关系
 *
 * @author lishangbu
 * @since 2025/8/10
 */
@Table
public class ItemAttributeRelation implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id private Long id;

  private Integer itemId;

  private Integer itemAttributeId;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Integer getItemId() {
    return itemId;
  }

  public void setItemId(Integer itemId) {
    this.itemId = itemId;
  }

  public Integer getItemAttributeId() {
    return itemAttributeId;
  }

  public void setItemAttributeId(Integer itemAttributeId) {
    this.itemAttributeId = itemAttributeId;
  }
}

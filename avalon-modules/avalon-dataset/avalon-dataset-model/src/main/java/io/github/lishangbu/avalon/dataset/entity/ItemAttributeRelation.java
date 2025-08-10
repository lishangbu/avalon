package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.data.jdbc.id.AutoLongIdGenerator;
import java.io.Serial;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 道具属性关系
 *
 * @author lishangbu
 * @since 2025/8/10
 */
@Table
public class ItemAttributeRelation implements AutoLongIdGenerator {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id private Long id;

  private Integer itemId;

  private Integer itemAttributeId;

  @Override
  public Long getId() {
    return id;
  }

  @Override
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

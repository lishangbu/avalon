package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.data.jdbc.id.AutoLongIdGenerator;
import java.io.Serial;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 招式学习器
 *
 * @author lishangbu
 * @since 2025/6/26
 */
@Table
public class Machine implements AutoLongIdGenerator {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id private Long id;

  private Integer itemId;

  private Integer moveId;

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

  public Integer getMoveId() {
    return moveId;
  }

  public void setMoveId(Integer moveId) {
    this.moveId = moveId;
  }
}

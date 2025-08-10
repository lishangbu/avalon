package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 道具类别
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@Table
public class ItemCategory implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id private Integer id;

  /**
   * 内部名称
   *
   * <p>取百科中的分类数据
   */
  private String internalName;

  /** 名称 */
  private String name;

  /** 该类别道具所属的口袋 */
  private String itemPocketInternalName;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getItemPocketInternalName() {
    return itemPocketInternalName;
  }

  public void setItemPocketInternalName(String itemPocketInternalName) {
    this.itemPocketInternalName = itemPocketInternalName;
  }
}

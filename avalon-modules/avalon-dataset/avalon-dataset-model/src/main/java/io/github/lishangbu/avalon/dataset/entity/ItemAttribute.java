package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 道具类别
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@Comment("道具属性")
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_item_attribute_internal_name",
          columnNames = {"internal_name"})
    })
public class ItemAttribute implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id
  @Comment("主键")
  private Integer id;

  /**
   * 内部名称
   *
   * <p>取百科中的分类数据
   */
  @Column(nullable = false, length = 20)
  @ColumnDefault("''")
  @Comment("内部名称")
  private String internalName;

  /** 名称 */
  @Comment("名称")
  @Column(length = 20, nullable = false)
  private String name;

  /** 道具属性描述 */
  @Comment("描述")
  @Column(length = 50, nullable = false)
  private String description;

  /** 具有该属性的道具列表,外键表由ITEM维护 */
  @ManyToMany(mappedBy = "attributes")
  private List<Item> items;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Item> getItems() {
    return items;
  }

  public void setItems(List<Item> items) {
    this.items = items;
  }
}

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
@Comment("道具口袋")
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_item_pocket_internal_name",
          columnNames = {"internal_name"})
    })
public class ItemPocket implements Serializable {
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

  /** 该口袋下具有的道具分类 */
  @OneToMany(mappedBy = "itemPocket")
  private List<ItemCategory> itemCategories;

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

  public List<ItemCategory> getItemCategories() {
    return itemCategories;
  }

  public void setItemCategories(List<ItemCategory> itemCategories) {
    this.itemCategories = itemCategories;
  }
}

package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 道具"投掷"效果
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@Comment("道具投掷效果")
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_item_fling_effect_internal_name",
          columnNames = {"internal_name"})
    })
public class ItemFlingEffect implements Serializable {
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
  @Column(length = 30, nullable = false)
  private String name;

  /** 道具“投掷”效果 */
  @Comment("效果")
  @Column(length = 100, nullable = false)
  private String effect;

  /** 具有此投掷效果的道具列表 */
  @OneToMany(mappedBy = "flingEffect")
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

  public String getEffect() {
    return effect;
  }

  public void setEffect(String effect) {
    this.effect = effect;
  }

  public List<Item> getItems() {
    return items;
  }

  public void setItems(List<Item> items) {
    this.items = items;
  }
}

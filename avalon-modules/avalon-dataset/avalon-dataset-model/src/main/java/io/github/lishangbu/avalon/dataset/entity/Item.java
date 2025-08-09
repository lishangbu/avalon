package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 道具
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@Comment("道具")
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_item_internal_name",
          columnNames = {"internal_name"})
    })
public class Item implements Serializable {
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
  @Column(nullable = false, length = 100)
  @ColumnDefault("''")
  @Comment("内部名称")
  private String internalName;

  /** 名称 */
  @Comment("名称")
  @Column(length = 100, nullable = false)
  private String name;

  @Comment("在商店中的价格")
  @Column(nullable = false)
  private Integer cost;

  @Comment("使用此道具进行投掷行动时的威力")
  private Integer flingPower;

  @ManyToOne
  @JoinColumn(name = "fling_effect_id", foreignKey = @ForeignKey(name = "fk_item_fling_effect_id"))
  @Comment("使用此道具进行投掷行动时的效果")
  private ItemFlingEffect flingEffect;

  /** 此道具具有的属性列表 */
  @JoinTable(
      name = "item_attribute_relation",
      joinColumns =
          @JoinColumn(
              name = "item_id",
              foreignKey = @ForeignKey(name = "fk_item_attribute_relation_item_id")),
      inverseJoinColumns =
          @JoinColumn(
              name = "item_attribute_id",
              foreignKey = @ForeignKey(name = "fk_item_attribute_relation_item_attribute_id")))
  @ManyToMany
  private List<ItemAttribute> attributes;

  /** 此道具所属的类别 */
  @ManyToOne
  @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_item_category_id"))
  private ItemCategory category;

  @Comment("简要效果描述")
  private String shortEffect;

  @Comment("效果描述")
  @Column(length = 1000)
  private String effect;

  @Comment("道具文本")
  private String text;

  @OneToOne(mappedBy = "item")
  private Machine machine;

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

  public Integer getCost() {
    return cost;
  }

  public void setCost(Integer cost) {
    this.cost = cost;
  }

  public Integer getFlingPower() {
    return flingPower;
  }

  public void setFlingPower(Integer flingPower) {
    this.flingPower = flingPower;
  }

  public ItemFlingEffect getFlingEffect() {
    return flingEffect;
  }

  public void setFlingEffect(ItemFlingEffect flingEffect) {
    this.flingEffect = flingEffect;
  }

  public List<ItemAttribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<ItemAttribute> attributes) {
    this.attributes = attributes;
  }

  public ItemCategory getCategory() {
    return category;
  }

  public void setCategory(ItemCategory category) {
    this.category = category;
  }

  public String getShortEffect() {
    return shortEffect;
  }

  public void setShortEffect(String shortEffect) {
    this.shortEffect = shortEffect;
  }

  public String getEffect() {
    return effect;
  }

  public void setEffect(String effect) {
    this.effect = effect;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Machine getMachine() {
    return machine;
  }

  public void setMachine(Machine machine) {
    this.machine = machine;
  }
}

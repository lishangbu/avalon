package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 道具
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@Table
public class Item implements Serializable {
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

  /** 在商店中的价格 */
  private Integer cost;

  /** 使用此道具进行投掷行动时的威力 */
  private Integer flingPower;

  /** 使用此道具进行投掷行动时的效果 */
  private String flingEffectInternalName;

  /** 此道具所属的类别 */
  private String categoryInternalName;

  /** 简要效果描述 */
  private String shortEffect;

  /** 效果描述 */
  private String effect;

  /** 道具文本 */
  private String text;

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

  public String getFlingEffectInternalName() {
    return flingEffectInternalName;
  }

  public void setFlingEffectInternalName(String flingEffectInternalName) {
    this.flingEffectInternalName = flingEffectInternalName;
  }

  public String getCategoryInternalName() {
    return categoryInternalName;
  }

  public void setCategoryInternalName(String categoryInternalName) {
    this.categoryInternalName = categoryInternalName;
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
}

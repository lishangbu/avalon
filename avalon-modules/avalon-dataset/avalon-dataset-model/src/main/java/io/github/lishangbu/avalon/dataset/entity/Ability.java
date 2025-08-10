package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 特性
 *
 * <p>特性是第三世代引入的每只宝可梦都拥有的特殊能力，可以在宝可梦的查看能力里查询。
 *
 * <p>特性在对战中或对战外具有特定的效果。大部分特性有利于宝可梦在对战中的发挥，但也存在懒惰、慢启动、软弱等不利于对战的特性。
 *
 * <p>灵活地运用特性可以在宝可梦对战、宝可梦培育、收服宝可梦等场合中获得意想不到的效果。
 *
 * @author lishangbu
 * @since 2025/4/17
 */
@Table
public class Ability implements Serializable {
  /** ID */
  @Id private Integer id;

  /**
   * 特性名称
   *
   * <p>取百科中的中文名数据
   */
  private String name;

  /**
   * 内部名称
   *
   * <p>取百科中的英文名数据
   */
  private String internalName;

  /** 文字介绍 */
  private String text;

  /** 基本信息 */
  private String info;

  /** 特性效果 */
  private String effect;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String code) {
    this.internalName = code;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getInfo() {
    return info;
  }

  public void setInfo(String info) {
    this.info = info;
  }

  public String getEffect() {
    return effect;
  }

  public void setEffect(String effect) {
    this.effect = effect;
  }
}

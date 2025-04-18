package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

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
@Entity
public class Ability implements Serializable {
  /** 内部ID */
  @Id private Integer id;

  /** 编号 */
  @Comment("编号")
  private String index;

  /**
   * 所属世代
   *
   * <p>每个特性只会属于一个世代
   *
   * @see Generation
   * @see Generation#getAbilities() ()
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "GENERATION_ID")
  private Generation generation;

  /**
   * 特性名称
   *
   * <p>取百科中的中文名数据
   */
  @Column(nullable = false, length = 100)
  @ColumnDefault("''")
  @Comment("特性名称")
  private String name;

  /**
   * 特性代码
   *
   * <p>取百科中的英文名数据
   */
  @Column(nullable = false, length = 100)
  @ColumnDefault("''")
  @Comment("特性代码")
  private String code;

  @Column(nullable = false, length = 500)
  @ColumnDefault("''")
  @Comment("文字介绍")
  private String text;

  @Column(nullable = false, length = 500)
  @ColumnDefault("''")
  @Comment("基本信息")
  private String info;

  /** 特性效果 */
  @Comment("特性效果")
  @ColumnDefault("''")
  @Column(length = 2000, nullable = false)
  private String effect;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }

  public Generation getGeneration() {
    return generation;
  }

  public void setGeneration(Generation generation) {
    this.generation = generation;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
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

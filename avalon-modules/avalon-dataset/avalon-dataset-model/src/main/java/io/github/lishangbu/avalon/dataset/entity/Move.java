package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 招式
 *
 * @author lishangbu
 * @since 2025/4/14
 */
@Comment("招式")
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_move_internal_name",
          columnNames = {"internal_name"})
    })
public class Move implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id
  @Comment("主键")
  private Integer id;

  /**
   * 招式名称
   *
   * <p>取百科中的中文名数据
   */
  @Comment("招式名称")
  private String name;

  /**
   * 内部名称
   *
   * <p>取百科中的招式英文名
   */
  @Column(nullable = false, length = 100)
  @ColumnDefault("''")
  @Comment("内部名称")
  private String internalName;

  /**
   * 属性
   *
   * <p>招式一般只能拥有一种属性。但一些招式的属性会因招式效果等因素而发生改变。特别地，飞身重压虽然是格斗属性的招式，但是却拥有格斗属性和飞行属性两种伤害属性效果。
   *
   * <p>每种属性都包含这几点要素：这种属性的招式（或招式造成伤害的属性）对哪些属性的宝可梦效果绝佳；对哪些属性的宝可梦效果不理想；对哪些属性的宝可梦没有效果。
   *
   * <p>《宝可梦》系列在第四世代之前，非变化招式的分类（即物理招式和特殊招式）取决于招式的属性；从第四世代起取决于招式本身。
   *
   * @see Type
   * @see Type#getMoves()
   */
  @ManyToOne
  @JoinColumn(name = "type_id")
  @Comment("属性")
  private Type type;

  /**
   * 分类
   *
   * @see MoveCategory
   * @see MoveCategory#getMoves()
   */
  @Comment("招式分类")
  @JoinColumn(name = "category_id")
  @ManyToOne
  private MoveCategory category;

  /**
   * 威力是一个招式可以造成伤害的衡量数值。
   *
   * <p>有些招式并不造成伤害，或者威力不定，亦或是一击必杀招式等等，这样的招式在威力一栏显示为“—”。
   *
   * <p>变化招式:所有的变化招式都不直接造成伤害，故威力为“—”。
   *
   * <p>固定伤害类招式:固定伤害招式不通过威力计算伤害，而是对其造成固定数值的伤害。比如：音爆。
   *
   * <p>威力不定的招式:威力不定的招式的威力由其他方式决定，像是精神波，它造成的伤害以招式使用方的等级决定。
   *
   * <p>一击必杀的招式:一击必杀的招式一旦命中，对方一般会即刻进入濒死状态。
   *
   * <p>特别的招式:部分攻击Ｚ招式、极巨招式和部分超极巨招式的威力会根据原本招式的威力计算。通过精通招式后以迅疾或刚猛使出的招式威力会发生变化。
   */
  @Comment("威力")
  private Integer power;

  /**
   * 命中
   *
   * <p>命中是衡量一个招式是否容易击中对方的数值。
   */
  private Integer accuracy;

  /**
   * 招式点数(Power Point）
   *
   * <p>说明了一个招式可以被使用的次数
   */
  @Comment("招式点数")
  private Integer pp;

  /** 文本描述 */
  @Comment("文本描述")
  private String text;

  /** 招式附加效果 */
  @Comment("招式附加效果")
  @ColumnDefault("''")
  @Column(length = 2000, nullable = false)
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

  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public MoveCategory getCategory() {
    return category;
  }

  public void setCategory(MoveCategory category) {
    this.category = category;
  }

  public Integer getPower() {
    return power;
  }

  public void setPower(Integer power) {
    this.power = power;
  }

  public Integer getAccuracy() {
    return accuracy;
  }

  public void setAccuracy(Integer accuracy) {
    this.accuracy = accuracy;
  }

  public Integer getPp() {
    return pp;
  }

  public void setPp(Integer pp) {
    this.pp = pp;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getEffect() {
    return effect;
  }

  public void setEffect(String effect) {
    this.effect = effect;
  }
}

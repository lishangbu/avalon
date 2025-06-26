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
   * 命中
   *
   * <p>命中是衡量一个招式是否容易击中对方的数值。
   */
  @Comment("命中")
  private Integer accuracy;

  @Comment("此招式效果发生的概率百分比值")
  private Integer effectChance;

  /**
   * 招式点数(Power Point）
   *
   * <p>说明了一个招式可以被使用的次数
   */
  @Comment("招式点数")
  private Integer pp;

  /** -8到8之间的值。设置战斗中招式执行的顺序 */
  @Comment("-8到8之间的值。设置战斗中招式执行的顺序")
  private Integer priority;

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
   * 此招式对目标造成的伤害类型
   *
   * @see MoveDamageClass
   * @see MoveDamageClass#getMoves()
   */
  @Comment("此招式对目标造成的伤害类型")
  @JoinColumn(name = "damage_class_id", foreignKey = @ForeignKey(name = "fk_move_damage_class_id"))
  @ManyToOne
  private MoveDamageClass damageClass;

  @Comment("此招式对目标造成的伤害类型")
  @JoinColumn(name = "target_id", foreignKey = @ForeignKey(name = "fk_move_target_id"))
  @ManyToOne
  private MoveTarget target;

  /** 文本描述 */
  @Comment("文本描述")
  @Column(length = 1000)
  private String text;

  /** 招式附加效果简要描述 */
  @Comment("招式简要效果描述")
  @Column(length = 1000)
  private String shortEffect;

  /** 招式附加效果 */
  @Comment("招式效果描述")
  @Column(length = 4000)
  private String effect;

  // region meta data
  /** 此招式持续生效的最小回合数。如果总是只持续一回合，则为空 */
  @Comment("此招式持续生效的最小回合数。如果总是只持续一回合，则为空")
  private Integer minHits;

  /** 此招式持续生效的最大回合数。如果总是只持续一回合，则为null */
  @Comment("此招式持续生效的最大回合数。如果总是只持续一回合，则为null")
  private Integer maxTurns;

  /** HP吸取（如果为正）或反作用伤害（如果为负），以造成伤害的百分比表示 */
  @Comment("HP吸取（如果为正）或反作用伤害（如果为负），以造成伤害的百分比表示")
  private Integer drain;

  /** 攻击方宝可梦恢复的HP量，以其最大HP的百分比表示 */
  @Comment("攻击方宝可梦恢复的HP量，以其最大HP的百分比表示")
  private Integer healing;

  /** 暴击率加成 */
  @Comment("暴击率加成")
  private Integer critRate;

  /** 此攻击导致状态异常的可能性 */
  @Comment("此攻击导致状态异常的可能性")
  private Integer ailmentChance;

  /** 此攻击导致目标宝可梦畏缩的可能性 */
  @Comment("此攻击导致目标宝可梦畏缩的可能性")
  private Integer flinchChance;

  /** 此攻击导致目标宝可梦能力值变化的可能性 */
  @Comment("此攻击导致目标宝可梦能力值变化的可能性")
  private Integer statChance;

  @ManyToOne
  @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_move_category_id"))
  private MoveCategory category;

  @ManyToOne
  @JoinColumn(name = "ailment_id", foreignKey = @ForeignKey(name = "fk_move_ailment_id"))
  private MoveAilment ailment;

  // endregion

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

  public Integer getAccuracy() {
    return accuracy;
  }

  public void setAccuracy(Integer accuracy) {
    this.accuracy = accuracy;
  }

  public Integer getEffectChance() {
    return effectChance;
  }

  public void setEffectChance(Integer effectChance) {
    this.effectChance = effectChance;
  }

  public Integer getPp() {
    return pp;
  }

  public void setPp(Integer pp) {
    this.pp = pp;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public Integer getPower() {
    return power;
  }

  public void setPower(Integer power) {
    this.power = power;
  }

  public MoveDamageClass getDamageClass() {
    return damageClass;
  }

  public void setDamageClass(MoveDamageClass damageClass) {
    this.damageClass = damageClass;
  }

  public MoveTarget getTarget() {
    return target;
  }

  public void setTarget(MoveTarget target) {
    this.target = target;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
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

  public Integer getMinHits() {
    return minHits;
  }

  public void setMinHits(Integer minHits) {
    this.minHits = minHits;
  }

  public Integer getMaxTurns() {
    return maxTurns;
  }

  public void setMaxTurns(Integer maxTurns) {
    this.maxTurns = maxTurns;
  }

  public Integer getDrain() {
    return drain;
  }

  public void setDrain(Integer drain) {
    this.drain = drain;
  }

  public Integer getHealing() {
    return healing;
  }

  public void setHealing(Integer healing) {
    this.healing = healing;
  }

  public Integer getCritRate() {
    return critRate;
  }

  public void setCritRate(Integer critRate) {
    this.critRate = critRate;
  }

  public Integer getAilmentChance() {
    return ailmentChance;
  }

  public void setAilmentChance(Integer ailmentChance) {
    this.ailmentChance = ailmentChance;
  }

  public Integer getFlinchChance() {
    return flinchChance;
  }

  public void setFlinchChance(Integer flinchChance) {
    this.flinchChance = flinchChance;
  }

  public Integer getStatChance() {
    return statChance;
  }

  public void setStatChance(Integer statChance) {
    this.statChance = statChance;
  }

  public MoveCategory getCategory() {
    return category;
  }

  public void setCategory(MoveCategory category) {
    this.category = category;
  }

  public MoveAilment getAilment() {
    return ailment;
  }

  public void setAilment(MoveAilment ailment) {
    this.ailment = ailment;
  }
}

package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 招式(Move)实体类
///
/// 表示宝可梦的招式信息，包括命中、威力、优先级及各种效果字段
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@Table(comment = "招式")
public class Move implements Serializable {
  @Serial private static final long serialVersionUID = -30304372663754761L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 内部名称
  @Column(comment = "内部名称", length = 100)
  private String internalName;

  /// 名称
  @Column(comment = "名称", length = 100)
  private String name;

  /// 属性
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(
      name = "type_id",
      referencedColumnName = "id",
      comment = "招式属性",
      foreignKey = @ForeignKey(name = "fk_move_type_id"))
  private Type type;

  /// 命中
  @Column(comment = "命中")
  private Integer accuracy;

  /// 此招式效果发生的概率百分比值
  @Column(comment = "此招式效果发生的概率百分比值")
  private Integer effectChance;

  /// 招式点数
  @Column(comment = "招式点数")
  private Integer pp;

  /// -8到8之间的值，设置战斗中招式执行的顺序
  @Column(comment = "-8到8之间的值。设置战斗中招式执行的顺序")
  private Integer priority;

  /// 威力
  @Column(comment = "威力")
  private Integer power;

  /// 此招式对目标造成的伤害类型
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(
      name = "damage_class_id",
      referencedColumnName = "id",
      comment = "此招式对目标造成的伤害类型",
      foreignKey = @ForeignKey(name = "fk_move_damage_class_id"))
  private MoveDamageClass moveDamageClass;

  /// 接收攻击效果的目标类型
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(
      name = "target_id",
      referencedColumnName = "id",
      comment = "接收攻击效果的目标类型",
      foreignKey = @ForeignKey(name = "fk_move_target_id"))
  private MoveTarget moveTarget;

  /// 文本描述
  @Column(comment = "文本描述", length = 500)
  private String text;

  /// 招式简要效果描述
  @Column(comment = "招式简要效果描述", length = 1000)
  private String shortEffect;

  /// 招式效果描述
  @Column(comment = "招式效果描述", length = 4000)
  private String effect;

  /// 此招式持续生效的最小回合数。如果总是只持续一回合，则为空
  @Column(comment = "此招式持续生效的最小回合数。如果总是只持续一回合，则为空")
  private Integer minHits;

  /// 此招式持续生效的最大回合数。如果总是只持续一回合，则为空
  @Column(comment = "此招式持续生效的最大回合数。如果总是只持续一回合，则为空")
  private Integer maxTurns;

  /// HP吸取（如果为正）或反作用伤害（如果为负），以造成伤害的百分比表示
  @Column(comment = "HP吸取（如果为正）或反作用伤害（如果为负），以造成伤害的百分比表示")
  private Integer drain;

  /// 攻击方宝可梦恢复的HP量，以其最大HP的百分比表示
  @Column(comment = "攻击方宝可梦恢复的HP量，以其最大HP的百分比表示")
  private Integer healing;

  /// 暴击率加成
  @Column(comment = "暴击率加成")
  private Integer critRate;

  /// 此攻击导致状态异常的可能性
  @Column(comment = "此攻击导致状态异常的可能性")
  private Integer ailmentChance;

  /// 此攻击导致目标宝可梦畏缩的可能性
  @Column(comment = "此攻击导致目标宝可梦畏缩的可能性")
  private Integer flinchChance;

  /// 此攻击导致目标宝可梦能力值变化的可能性
  ///
  /// {@link Move#effectChance} 目前这两个字段的值是相同的，可能是因为在某些版本中，这两种效果被分开计算了概率
  @Column(comment = "此攻击导致目标宝可梦能力值变化的可能性")
  private Integer statChance;

  /// 招式分类
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(
      name = "category_id",
      referencedColumnName = "id",
      comment = "招式分类",
      foreignKey = @ForeignKey(name = "fk_move_category_id"))
  private MoveCategory moveCategory;

  /// 招式导致的状态异常
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(
      name = "ailment_id",
      referencedColumnName = "id",
      comment = "招式导致的状态异常",
      foreignKey = @ForeignKey(name = "fk_move_ailment_id"))
  private MoveAilment moveAilment;
}

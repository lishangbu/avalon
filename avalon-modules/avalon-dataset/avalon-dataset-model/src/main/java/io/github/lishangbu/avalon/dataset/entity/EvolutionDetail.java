package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 进化细节(EvolutionDetail)实体类
///
/// 表示宝可梦进化的具体条件和要求
///
/// @author lishangbu
/// @since 2026/2/12
@Data
@Entity
@Table(comment = "进化细节")
public class EvolutionDetail implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 进化链环节 ID
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "chain_link_id",
      comment = "进化链环节ID",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_evolution_detail_chain_link"))
  private ChainLink chainLink;

  /// 进化触发器 ID
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "evolution_trigger_id",
      comment = "进化触发器ID",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_evolution_detail_evolution_trigger"))
  private EvolutionTrigger evolutionTrigger;

  /// 性别要求 (1: 雄性, 2: 雌性, null: 无要求)
  @Column(comment = "性别要求 (1: 雄性, 2: 雌性, null: 无要求)")
  private Integer gender;

  /// 持有物品 ID
  @Column(comment = "持有物品ID")
  private Long heldItemId;

  /// 进化物品 ID
  @Column(comment = "进化物品ID")
  private Long itemId;

  /// 已知招式 ID
  @Column(comment = "已知招式ID")
  private Long knownMoveId;

  /// 已知招式属性 ID
  @Column(comment = "已知招式属性ID")
  private Long knownMoveTypeId;

  /// 位置 ID
  @Column(comment = "位置ID")
  private Long locationId;

  /// 最小亲密度
  @Column(comment = "最小亲密度")
  private Integer minAffection;

  /// 最小美丽度
  @Column(comment = "最小美丽度")
  private Integer minBeauty;

  /// 最小快乐度
  @Column(comment = "最小快乐度")
  private Integer minHappiness;

  /// 最小等级
  @Column(comment = "最小等级")
  private Integer minLevel;

  /// 需要世界雨水
  @Column(comment = "需要世界雨水")
  private Boolean needsOverworldRain;

  /// 队伍中宝可梦种类 ID
  @Column(comment = "队伍中宝可梦种类ID")
  private Long partySpeciesId;

  /// 队伍中宝可梦属性 ID
  @Column(comment = "队伍中宝可梦属性ID")
  private Long partyTypeId;

  /// 相对物理属性 (-1: 防御高于攻击, 0: 相等, 1: 攻击高于防御)
  @Column(comment = "相对物理属性 (-1: 防御高于攻击, 0: 相等, 1: 攻击高于防御)")
  private Integer relativePhysicalStats;

  /// 时间段 (day: 白天, night: 夜晚)
  @Column(comment = "时间段 (day: 白天, night: 夜晚)", length = 10)
  private String timeOfDay;

  /// 交易宝可梦种类 ID
  @Column(comment = "交易宝可梦种类ID")
  private Long tradeSpeciesId;

  /// 倒转
  @Column(comment = "倒转")
  private Boolean turnUpsideDown;
}

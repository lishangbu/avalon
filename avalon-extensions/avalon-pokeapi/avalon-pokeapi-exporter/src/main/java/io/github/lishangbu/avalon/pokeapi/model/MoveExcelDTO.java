package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 招式 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/10
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveExcelDTO {
  /// 唯一标识
  @ExcelProperty("id")
  private Integer id;

  /// 内部名称
  @ExcelProperty("internal_name")
  private String internalName;

  /// 显示名称
  @ExcelProperty("name")
  private String name;

  /// 命中
  @ExcelProperty("accuracy")
  private Integer accuracy;

  /// 此招式效果发生的概率百分比值
  @ExcelProperty("effect_chance")
  private Integer effectChance;

  /// 招式点数
  @ExcelProperty("pp")
  private Integer pp;

  /// -8到8之间的值，设置战斗中招式执行的顺序
  @ExcelProperty("priority")
  private Integer priority;

  /// 威力
  @ExcelProperty("power")
  private Integer power;

  /// 文本
  @ExcelProperty("text")
  private String text;

  /// 招式简要效果描述
  @ExcelProperty("short_effect")
  private String shortEffect;

  /// 招式效果描述
  @ExcelProperty("effect")
  private String effect;

  /// 此招式持续生效的最小回合数。如果总是只持续一回合，则为空
  @ExcelProperty("min_hits")
  private Integer minHits;

  /// 此招式持续生效的最大回合数。如果总是只持续一回合，则为空
  @ExcelProperty("max_turns")
  private Integer maxTurns;

  /// HP吸取（如果为正）或反作用伤害（如果为负），以造成伤害的百分比表示
  @ExcelProperty("drain")
  private Integer drain;

  /// 攻击方宝可梦恢复的HP量，以其最大HP的百分比表示
  @ExcelProperty("healing")
  private Integer healing;

  /// 暴击率加成
  @ExcelProperty("crit_rate")
  private Integer critRate;

  /// 此攻击导致状态异常的可能性
  @ExcelProperty("ailment_chance")
  private Integer ailmentChance;

  /// 此攻击导致目标宝可梦畏缩的可能性
  @ExcelProperty("flinch_chance")
  private Integer flinchChance;

  /// 此攻击导致目标宝可梦能力值变化的可能性
  @ExcelProperty("stat_chance")
  private Integer statChance;

  /// 异常状态 ID
  @ExcelProperty("move_ailment_id")
  private Integer moveAilmentId;

  /// 招式分类 ID
  @ExcelProperty("move_category_id")
  private Integer moveCategoryId;

  /// 招式目标 ID
  @ExcelProperty("move_target_id")
  private Integer moveTargetId;

  /// 招式属性 ID
  @ExcelProperty("type_id")
  private Integer typeId;

  /// 招式伤害类型
  @ExcelProperty("move_damage_class_id")
  private Integer moveDamageClassId;
}

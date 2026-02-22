package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 进化链 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/10
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvolutionChainExcelDTO {

  /// 链起始物种ID
  @ExcelProperty("from_pokemon_species_id")
  private Integer fromPokemonSpeciesId;

  /// 链起始物种ID
  @ExcelProperty("to_pokemon_species_id")
  private Integer toPokemonSpeciesId;

  /// 道具ID
  @ExcelProperty("item_id")
  private Integer itemId;

  /// 触发器ID
  @ExcelProperty("trigger_id")
  private Integer triggerId;

  /// 性别
  @ExcelProperty("gender")
  private Integer gender;

  /// 持有道具ID
  @ExcelProperty("held_item_id")
  private Integer heldItemId;

  /// 已知招式ID
  @ExcelProperty("known_move_id")
  private Integer knownMoveId;

  /// 已知招式类型ID
  @ExcelProperty("known_move_type_id")
  private Integer knownMoveTypeId;

  /// 地点ID
  @ExcelProperty("location_id")
  private Integer locationId;

  /// 最小等级
  @ExcelProperty("min_level")
  private Integer minLevel;

  /// 最小幸福度
  @ExcelProperty("min_happiness")
  private Integer minHappiness;

  /// 最小美丽度
  @ExcelProperty("min_beauty")
  private Integer minBeauty;

  /// 最小亲密度
  @ExcelProperty("min_affection")
  private Integer minAffection;

  /// 需要多人游戏
  @ExcelProperty("needs_multiplayer")
  private Boolean needsMultiplayer;

  /// 需要户外雨天
  @ExcelProperty("needs_overworld_rain")
  private Boolean needsOverworldRain;

  /// 队伍物种ID
  @ExcelProperty("party_species_id")
  private Integer partySpeciesId;

  /// 队伍类型ID
  @ExcelProperty("party_type_id")
  private Integer partyTypeId;

  /// 相对物理属性
  @ExcelProperty("relative_physical_stats")
  private Integer relativePhysicalStats;

  /// 一天中的时间
  @ExcelProperty("time_of_day")
  private String timeOfDay;

  /// 交易物种ID
  @ExcelProperty("trade_species_id")
  private Integer tradeSpeciesId;

  /// 上下颠倒
  @ExcelProperty("turn_upside_down")
  private Boolean turnUpsideDown;

  /// 地区ID
  @ExcelProperty("region_id")
  private Integer regionId;

  /// 基础形态ID
  @ExcelProperty("base_form_id")
  private Integer baseFormId;

  /// 使用招式ID
  @ExcelProperty("used_move_id")
  private Integer usedMoveId;

  /// 最小招式数量
  @ExcelProperty("min_move_count")
  private Integer minMoveCount;

  /// 最小步数
  @ExcelProperty("min_steps")
  private Integer minSteps;

  /// 最小伤害承受
  @ExcelProperty("min_damage_taken")
  private Integer minDamageTaken;
}

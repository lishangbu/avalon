package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 宝可梦种类 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/10
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PokemonSpeciesExcelDTO {
  /// 唯一标识
  @ExcelProperty("id")
  private Integer id;

  /// 内部名称
  @ExcelProperty("internal_name")
  private String internalName;

  /// 显示名称
  @ExcelProperty("name")
  private String name;

  /// 排序顺序
  @ExcelProperty("sorting_order")
  private Integer sortingOrder;

  /// 性别比例
  @ExcelProperty("gender_rate")
  private Integer genderRate;

  /// 捕获率
  @ExcelProperty("capture_rate")
  private Integer captureRate;

  /// 基础幸福度
  @ExcelProperty("base_happiness")
  private Integer baseHappiness;

  /// 是否为幼年形态
  @ExcelProperty("is_baby")
  private Boolean isBaby;

  /// 是否为传说宝可梦
  @ExcelProperty("is_legendary")
  private Boolean isLegendary;

  /// 是否为神话宝可梦
  @ExcelProperty("is_mythical")
  private Boolean isMythical;

  /// 孵化周期
  @ExcelProperty("hatch_counter")
  private Integer hatchCounter;

  /// 是否有性别差异
  @ExcelProperty("has_gender_differences")
  private Boolean hasGenderDifferences;

  /// 形态是否可切换
  @ExcelProperty("forms_switchable")
  private Boolean formsSwitchable;

  /// 成长速率 ID
  @ExcelProperty("growth_rate_id")
  private Integer growthRateId;

  /// 颜色 ID
  @ExcelProperty("color_id")
  private Integer colorId;

  /// 形状 ID
  @ExcelProperty("shape_id")
  private Integer shapeId;

  /// 进化来源种类 ID
  @ExcelProperty("evolves_from_species_id")
  private Integer evolvesFromSpeciesId;

  /// 进化链 ID
  @ExcelProperty("evolution_chain_id")
  private Integer evolutionChainId;

  /// 栖息地 ID
  @ExcelProperty("habitat_id")
  private Integer habitatId;

  /// 世代 ID
  @ExcelProperty("generation_id")
  private Integer generationId;
}

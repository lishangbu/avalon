package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 宝可梦 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/10
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PokemonExcelDTO {
  /// 唯一标识
  @ExcelProperty("id")
  private Integer id;

  /// 内部名称
  @ExcelProperty("internal_name")
  private String internalName;

  /// 显示名称
  @ExcelProperty("name")
  private String name;

  /// 基础经验值
  @ExcelProperty("base_experience")
  private Integer baseExperience;

  /// 身高（分米）
  @ExcelProperty("height")
  private Integer height;

  /// 是否为默认
  @ExcelProperty("is_default")
  private Boolean isDefault;

  /// 排序
  @ExcelProperty("sorting_order")
  private Integer sortingOrder;

  /// 体重（百克）
  @ExcelProperty("weight")
  private Integer weight;

  /// 种类 ID
  @ExcelProperty("pokemon_species_id")
  private Integer pokemonSpeciesId;
}

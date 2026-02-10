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
  /// 唯一标识
  @ExcelProperty("id")
  private Integer id;

  /// 幼年触发道具名称
  @ExcelProperty("baby_trigger_item_name")
  private String babyTriggerItemName;

  /// 链起始物种名称
  @ExcelProperty("chain_species_name")
  private String chainSpeciesName;
}

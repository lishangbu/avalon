package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 特征 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/10
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharacteristicExcelDTO {
  /// 唯一标识
  @ExcelProperty("id")
  private Integer id;

  /// 最高 IV 除以 5 的余数
  @ExcelProperty("gene_modulo")
  private Integer geneModulo;

  /// 导致该特征的最高 IV 的可能值列表
  @ExcelProperty("possible_values")
  private String possibleValues;

  /// 导致该特征的属性 ID
  @ExcelProperty("highest_stat_id")
  private Integer highestStatId;

  /// 描述
  @ExcelProperty("description")
  private String description;
}

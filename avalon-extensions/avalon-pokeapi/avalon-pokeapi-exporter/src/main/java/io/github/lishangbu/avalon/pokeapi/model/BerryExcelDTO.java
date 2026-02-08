package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 树果 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/4
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BerryExcelDTO {
  /// 主键信息
  @ExcelProperty("id")
  private Integer id;

  /// 内部名称
  @ExcelProperty("internal_name")
  private String internalName;

  /// 显示名称
  @ExcelProperty("name")
  private String name;

  /// 生长时间
  @ExcelProperty("growth_time")
  private Integer growthTime;

  /// 最大结果数
  @ExcelProperty("max_harvest")
  private Integer maxHarvest;

  /// 体积
  @ExcelProperty("bulk")
  private Integer bulk;

  /// 光滑度
  @ExcelProperty("smoothness")
  private Integer smoothness;

  /// 土壤干燥度
  @ExcelProperty("soil_dryness")
  private Integer soilDryness;

  /// 树果硬度ID
  @ExcelProperty("firmness_id")
  private Integer firmnessId;

  /// 自然之恩招式属性
  @ExcelProperty("natural_gift_type_id")
  private Integer naturalGiftTypeId;

  /// 自然之恩招式威力
  @ExcelProperty("natural_gift_power")
  private Integer naturalGiftPower;
}

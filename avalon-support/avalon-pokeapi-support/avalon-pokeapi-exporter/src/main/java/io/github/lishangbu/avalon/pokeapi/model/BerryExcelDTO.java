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
  @ExcelProperty("ID")
  private Integer id;

  /// 内部名称
  @ExcelProperty("内部名称")
  private String internalName;

  /// 显示名称
  @ExcelProperty("显示名称")
  private String name;

  /// 生长时间
  @ExcelProperty("生长时间")
  private Integer growthTime;

  /// 最大收获
  @ExcelProperty("最大收获")
  private Integer maxHarvest;

  /// 体积
  @ExcelProperty("体积")
  private Integer bulk;

  /// 光滑度
  @ExcelProperty("光滑度")
  private Integer smoothness;

  /// 土壤干燥度
  @ExcelProperty("土壤干燥度")
  private Integer soilDryness;

  /// 硬度内部名称
  @ExcelProperty("硬度内部名称")
  private String firmnessInternalName;

  /// 自然之恩招式属性
  @ExcelProperty("自然之恩招式属性")
  private String naturalGiftTypeInternalName;

  /// 自然之恩招式威力
  @ExcelProperty("自然之恩招式威力")
  private Integer naturalGiftPower;
}

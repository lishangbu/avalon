package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 树果风味 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/4
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BerryFlavorExcelDTO {
  /// 主键信息
  @ExcelProperty("ID")
  private Integer id;

  /// 内部名称
  @ExcelProperty("内部名称")
  private String internalName;

  /// 显示名称
  @ExcelProperty("显示名称")
  private String name;
}

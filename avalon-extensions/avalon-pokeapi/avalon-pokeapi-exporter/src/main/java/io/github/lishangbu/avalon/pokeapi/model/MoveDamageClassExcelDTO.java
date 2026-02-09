package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 招式伤害类别 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/4
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveDamageClassExcelDTO {
  /// 唯一标识
  @ExcelProperty("id")
  private Integer id;

  /// 内部名称
  @ExcelProperty("internal_name")
  private String internalName;

  /// 显示名称
  @ExcelProperty("name")
  private String name;

  /// 描述文本
  @ExcelProperty("description")
  private String description;
}

package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 道具属性关联 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/16
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemAttributeRelationExcelDTO {
  /// 道具ID
  @ExcelProperty("item_id")
  private Integer itemId;

  /// 属性ID
  @ExcelProperty("attribute_id")
  private Integer attributeId;
}

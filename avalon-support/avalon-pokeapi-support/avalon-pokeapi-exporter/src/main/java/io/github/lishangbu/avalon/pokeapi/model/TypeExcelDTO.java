package io.github.lishangbu.avalon.pokeapi.model;

import org.apache.fesod.sheet.annotation.ExcelProperty;

/// 属性模型Excel数据传输对象
///
/// @param id                  资源标识符
/// @param name                资源名称
/// @author lishangbu
/// @since 2026/2/4
public record TypeExcelDTO(
  @ExcelProperty("ID")
  Integer id,
  @ExcelProperty("内部名称")
  String name) {}

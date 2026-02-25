package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 性别 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/4
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenderExcelDTO {
    /// 唯一标识
    @ExcelProperty("id")
    private Integer id;

    /// 内部名称
    @ExcelProperty("internal_name")
    private String internalName;
}

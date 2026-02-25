package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 机器 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/10
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineExcelDTO {
    /// 唯一标识
    @ExcelProperty("id")
    private Integer id;

    /// 道具名称
    @ExcelProperty("item_name")
    private String itemName;

    /// 招式名称
    @ExcelProperty("move_name")
    private String moveName;

    /// 版本组名称
    @ExcelProperty("version_group_name")
    private String versionGroupName;
}

package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 性格 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/10
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NatureExcelDTO {
    /// 唯一标识
    @ExcelProperty("id")
    private Integer id;

    /// 内部名称
    @ExcelProperty("internal_name")
    private String internalName;

    /// 显示名称
    @ExcelProperty("name")
    private String name;

    /// 降低 10% 的属性 ID
    @ExcelProperty("decreased_stat_id")
    private Integer decreasedStatId;

    /// 增加 10% 的属性 ID
    @ExcelProperty("increased_stat_id")
    private Integer increasedStatId;

    /// 讨厌的口味 ID
    @ExcelProperty("hates_berry_flavor_id")
    private Integer hatesFlavorId;

    /// 喜欢的口味 ID
    @ExcelProperty("likes_berry_flavor_id")
    private Integer likesFlavorId;
}

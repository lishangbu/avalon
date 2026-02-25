package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 道具 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/10
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemExcelDTO {
    /// 唯一标识
    @ExcelProperty("id")
    private Integer id;

    /// 内部名称
    @ExcelProperty("internal_name")
    private String internalName;

    /// 显示名称
    @ExcelProperty("name")
    private String name;

    /// 价格
    @ExcelProperty("cost")
    private Integer cost;

    /// 投掷威力
    @ExcelProperty("fling_power")
    private Integer flingPower;

    @ExcelProperty("item_fling_effect_id")
    private Integer itemFlingEffect;

    /// 类别名称
    @ExcelProperty("category_id")
    private Integer categoryId;

    /// 效果描述
    @ExcelProperty("effect")
    private String effect;

    /// 简短效果描述
    @ExcelProperty("short_effect")
    private String shortEffect;

    /// 道具文本
    @ExcelProperty("text")
    private String text;
}

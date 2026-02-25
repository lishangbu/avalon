package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 属性 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/10
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatExcelDTO {
    /// 唯一标识
    @ExcelProperty("id")
    private Integer id;

    /// 内部名称
    @ExcelProperty("internal_name")
    private String internalName;

    /// 显示名称
    @ExcelProperty("name")
    private String name;

    /// 游戏侧用于此属性的 ID
    @ExcelProperty("game_index")
    private Integer gameIndex;

    /// 此属性是否仅在战斗中存在
    @ExcelProperty("is_battle_only")
    private Boolean isBattleOnly;

    /// 与此属性相关的伤害类别 ID
    @ExcelProperty("move_damage_class_id")
    private Integer moveDamageClassId;
}

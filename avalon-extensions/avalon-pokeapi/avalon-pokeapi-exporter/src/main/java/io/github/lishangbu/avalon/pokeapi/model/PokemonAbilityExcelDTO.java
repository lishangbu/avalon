package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 宝可梦能力 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/15
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PokemonAbilityExcelDTO {
    /// 唯一标识
    @ExcelProperty("ability_id")
    private Integer abilityId;

    /// 内部名称
    @ExcelProperty("pokemon_id")
    private Integer pokemonId;

    /// 是否隐藏特性
    @ExcelProperty("is_hidden")
    private Boolean isHidden;

    /// 特性槽位（1-第一槽位，2-第二槽位，3-第三槽位）
    @ExcelProperty("slot")
    private Integer slot;
}

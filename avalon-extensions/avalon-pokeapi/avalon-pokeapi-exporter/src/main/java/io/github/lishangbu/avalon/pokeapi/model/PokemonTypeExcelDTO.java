package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 宝可梦属性 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/16
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PokemonTypeExcelDTO {
    /// 宝可梦ID
    @ExcelProperty("pokemon_id")
    private Integer pokemonId;

    /// 属性ID
    @ExcelProperty("type_id")
    private Integer typeId;

    /// 属性slot
    @ExcelProperty("slot")
    private Integer slot;
}

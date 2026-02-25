package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 宝可梦能力值 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/16
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PokemonStatExcelDTO {
    /// 宝可梦ID
    @ExcelProperty("pokemon_id")
    private Integer pokemonId;

    /// 属性ID
    @ExcelProperty("stat_id")
    private Integer statId;

    /// 基础能力值
    @ExcelProperty("base_stat")
    private Integer baseStat;

    /// 努力值
    @ExcelProperty("effort")
    private Integer effort;
}

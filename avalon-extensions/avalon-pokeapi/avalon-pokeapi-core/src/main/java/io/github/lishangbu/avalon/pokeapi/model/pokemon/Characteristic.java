package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 特征模型
///
/// 表示哪个属性包含宝可梦的最高个体值 (IV)，详情参考 [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Characteristic)
///
/// @param id             资源标识符
/// @param geneModulo     最高 IV 除以 5 的余数
/// @param possibleValues 导致该特征的最高 IV 的可能值列表
/// @param highestStat    导致该特征的属性引用
/// @param descriptions   多语言描述列表
/// @author lishangbu
/// @see Stat
/// @see Description
/// @since 2025/6/8
public record Characteristic(
        Integer id,
        @JsonProperty("gene_modulo") Integer geneModulo,
        @JsonProperty("possible_values") List<Integer> possibleValues,
        @JsonProperty("highest_stat") NamedApiResource<Stat> highestStat,
        List<Description> descriptions) {}

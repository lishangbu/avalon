package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 特征表示哪个属性包含宝可梦的最高个体值(IV)。宝可梦的特征由其最高IV除以5的余数(gene_modulo)决定。更多详情请查阅<a
 * href="http://bulbapedia.bulbagarden.net/wiki/Characteristic">Bulbapedia</a>。
 *
 * @param id 该资源的标识符
 * @param geneModulo 最高属性/IV除以5的余数
 * @param possibleValues 除以5后会导致宝可梦获得此特征的最高属性的可能值列表
 * @param highestStat 产生此特征的属性{@link Stat}
 * @param descriptions 此特征在不同语言中的描述列表{@link Description}
 * @author lishangbu
 * @see Stat
 * @see Description
 * @since 2025/6/8
 */
public record Characteristic(
    Integer id,
    @JsonProperty("gene_modulo") Integer geneModulo,
    @JsonProperty("possible_values") List<Integer> possibleValues,
    @JsonProperty("highest_stat") NamedApiResource<Stat> highestStat,
    List<Description> descriptions) {}

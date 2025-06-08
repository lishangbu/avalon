package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.common.VerboseEffect;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import java.util.List;

/**
 * 特性为宝可梦在战斗中或在主世界中提供被动效果。宝可梦可能拥有多种可能的特性，但一次只能拥有一种特性。 查看 <a
 * href="http://bulbapedia.bulbagarden.net/wiki/Ability">Bulbapedia</a> 获取更多详情。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param isMainSeries 此特性是否起源于视频游戏的主系列
 * @param generation 此特性起源的游戏世代{@link Generation}
 * @param names 该资源在不同语言中列出的名称{@link Name}
 * @param effectEntries 此特性在不同语言中列出的效果{@link VerboseEffect}
 * @param effectChanges 此特性在不同版本组中曾经有过的先前效果{@link AbilityEffectChange}列表
 * @param flavorTextEntries 此特性在不同语言中列出的风味文本{@link AbilityFlavorText}
 * @param pokemon 可能拥有此特性的宝可梦{@link AbilityPokemon}列表
 * @author lishangbu
 * @see Generation
 * @see Name
 * @see VerboseEffect
 * @see AbilityEffectChange
 * @see AbilityFlavorText
 * @see AbilityPokemon
 * @since 2025/6/8
 */
public record Ability(
    Integer id,
    String name,
    @JsonProperty("is_main_series") Boolean isMainSeries,
    NamedApiResource<Generation> generation,
    List<Name> names,
    @JsonProperty("effect_entries") List<VerboseEffect> effectEntries,
    @JsonProperty("effect_changes") List<AbilityEffectChange> effectChanges,
    @JsonProperty("flavor_text_entries") List<AbilityFlavorText> flavorTextEntries,
    List<AbilityPokemon> pokemon) {}

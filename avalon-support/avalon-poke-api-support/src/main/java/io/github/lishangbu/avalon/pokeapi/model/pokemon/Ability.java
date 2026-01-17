package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.common.VerboseEffect;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import java.util.List;

/// 特性模型
///
/// 特性为宝可梦提供被动效果，详情参考 [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Ability)
///
/// @param id                资源标识符
/// @param name              资源名称
/// @param isMainSeries      是否起源于主系列游戏
/// @param generation        特性起源的世代引用
/// @param names             多语言名称列表
/// @param effectEntries     多语言效果描述
/// @param effectChanges     不同版本组下的先前效果列表
/// @param flavorTextEntries 风味文本（多语言）
/// @param pokemon           可能拥有此特性的宝可梦列表
/// @author lishangbu
/// @see Generation
/// @see Name
/// @see VerboseEffect
/// @see AbilityEffectChange
/// @see AbilityFlavorText
/// @see AbilityPokemon
/// @since 2025/6/8
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

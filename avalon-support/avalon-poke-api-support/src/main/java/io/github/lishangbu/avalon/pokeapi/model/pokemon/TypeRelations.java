package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 类型相克关系模型
///
/// 参考 PokeAPI: [Pokémon/Types/TypeRelations](https://pokeapi.co/docs/v2)
///
/// @param noDamageTo       此属性无效的属性列表
/// @param halfDamageTo     此属性效果较弱的属性列表
/// @param doubleDamageTo   此属性效果强的属性列表
/// @param noDamageFrom     对该属性无效的属性列表
/// @param halfDamageFrom   对该属性效果较弱的属性列表
/// @param doubleDamageFrom 对该属性效果强的属性列表
/// @author lishangbu
/// @since 2025/5/20
public record TypeRelations(
    @JsonProperty("no_damage_to") List<NamedApiResource<Type>> noDamageTo,
    @JsonProperty("half_damage_to") List<NamedApiResource<Type>> halfDamageTo,
    @JsonProperty("double_damage_to") List<NamedApiResource<Type>> doubleDamageTo,
    @JsonProperty("no_damage_from") List<NamedApiResource<Type>> noDamageFrom,
    @JsonProperty("half_damage_from") List<NamedApiResource<Type>> halfDamageFrom,
    @JsonProperty("double_damage_from") List<NamedApiResource<Type>> doubleDamageFrom) {}

package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Pokémon/Types/TypeRelationsPast</a>
 *
 * @param generation 引用属性在最后一代中的伤害关系
 * @param damageRelations 引用属性在该代及之前所有代中的伤害关系
 * @author lishangbu
 * @since 2025/5/20
 */
public record TypeRelationsPast<T>(
    NamedApiResource<?> generation,
    @JsonProperty("damage_relations") TypeRelations damageRelations) {}

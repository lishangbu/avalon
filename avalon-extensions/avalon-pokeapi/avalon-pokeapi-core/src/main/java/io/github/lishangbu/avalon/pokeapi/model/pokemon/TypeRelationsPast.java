package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 历史属性关系模型（TypeRelationsPast）
///
/// @param generation      引用属性在特定世代的信息
/// @param damageRelations 引用属性在该世代及之前的伤害关系
/// @author lishangbu
/// @since 2025/5/20
public record TypeRelationsPast<T>(
        NamedApiResource<?> generation,
        @JsonProperty("damage_relations") TypeRelations damageRelations) {}

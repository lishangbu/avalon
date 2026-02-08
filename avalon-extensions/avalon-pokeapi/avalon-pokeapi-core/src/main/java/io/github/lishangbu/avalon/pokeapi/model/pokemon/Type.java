package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.GenerationGameIndex;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 属性模型
///
/// 表示宝可梦与招式的属性类型及其相互关系（例如克制/被克制关系）
///
/// @param id                  资源标识符
/// @param name                资源名称
/// @param damageRelations     与其他属性间的相互作用关系
/// @param pastDamageRelations 之前世代的属性关系记录
/// @param gameIndices         各代游戏索引
/// @param generation          首次出现的世代引用
/// @param moveDamageClass     属性对应的伤害类别引用
/// @param names               多语言名称列表
/// @param pokemon             拥有此属性的宝可梦列表
/// @param moves               拥有此属性的招式列表
/// @author lishangbu
/// @since 2025/5/20
@JsonIgnoreProperties(ignoreUnknown = true)
public record Type(
    Integer id,
    String name,
    @JsonProperty("damage_relations") TypeRelations damageRelations,
    @JsonProperty("past_damage_relations") List<TypeRelationsPast<?>> pastDamageRelations,
    @JsonProperty("game_indices") List<GenerationGameIndex> gameIndices,
    NamedApiResource<?> generation,
    @JsonProperty("move_damage_class") NamedApiResource<?> moveDamageClass,
    List<Name> names,
    List<TypePokemon> pokemon,
    List<NamedApiResource<?>> moves) {}

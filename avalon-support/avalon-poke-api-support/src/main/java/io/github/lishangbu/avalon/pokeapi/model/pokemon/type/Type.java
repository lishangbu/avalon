package io.github.lishangbu.avalon.pokeapi.model.pokemon.type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.GenerationGameIndex;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 属性是宝可梦及其招式的属性。每种类型有三个特性：对哪些类型的宝可梦效果绝佳、对哪些类型的宝可梦效果不佳，以及对哪些类型的宝可梦完全无效。
 *
 * @param id 此资源的标识符
 * @param name 此资源的名称
 * @param damageRelations 此属性与其他属性之间的有效性关系
 * @param pastDamageRelations 此属性在之前世代中的有效性关系
 * @param gameIndices 与此项相关的各代游戏索引
 * @param generation 此属性首次出现的世代
 * @param moveDamageClass 此属性的伤害类别
 * @param names 此资源在不同语言中的名称
 * @param pokemon 拥有此属性的宝可梦列表
 * @param moves 具有此属性的招式列表
 * @author lishangbu
 * @since 2025/5/20
 */
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

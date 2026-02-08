package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.common.VersionGameIndex;
import java.util.List;

/// 宝可梦模型
///
/// 表示栖息在宝可梦游戏世界中的生物，包含多语言描述、形态、招式与历史属性等信息（参考
// [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Pok%C3%A9mon_(species))）

/// @param id                     该资源的标识符
/// @param name                   该资源的名称
/// @param baseExperience         击败此宝可梦获得的基础经验值
/// @param height                 此宝可梦的身高（以分米为单位）
/// @param isDefault              是否为该物种的默认宝可梦
/// @param order                  用于排序的顺序
/// @param weight                 体重（以百克为单位）
/// @param abilities              可能拥有的特性列表
/// @param forms                  可采取的形态列表
/// @param gameIndices            各世代的游戏索引列表
/// @param heldItems              可能持有的道具列表
/// @param locationAreaEncounters 指向地点区域与遭遇详情的链接
/// @param moves                  招式列表
/// @param pastTypes              先前世代的属性列表
/// @param pastAbilities          先前世代的特性列表
/// @param sprites                精灵图像集合
/// @param cries                  叫声集合
/// @param species                所属物种引用
/// @param stats                  基础属性值列表
/// @param types                  属性类型列表
/// @author lishangbu
/// @see PokemonAbility
/// @see PokemonForm
/// @see VersionGameIndex
/// @see PokemonHeldItem
/// @see PokemonMove
/// @see PokemonTypePast
/// @see PokemonAbilityPast
/// @see PokemonSprites
/// @see PokemonCries
/// @see PokemonSpecies
/// @see PokemonStat
/// @see PokemonType
/// @since 2025/6/8
public record Pokemon(
    Integer id,
    String name,
    @JsonProperty("base_experience") Integer baseExperience,
    Integer height,
    @JsonProperty("is_default") Boolean isDefault,
    Integer order,
    Integer weight,
    List<PokemonAbility> abilities,
    List<NamedApiResource<PokemonForm>> forms,
    @JsonProperty("game_indices") List<VersionGameIndex> gameIndices,
    @JsonProperty("held_items") List<PokemonHeldItem> heldItems,
    @JsonProperty("location_area_encounters") String locationAreaEncounters,
    List<PokemonMove> moves,
    @JsonProperty("past_types") List<PokemonTypePast> pastTypes,
    @JsonProperty("past_abilities") List<PokemonAbilityPast> pastAbilities,
    PokemonSprites sprites,
    PokemonCries cries,
    NamedApiResource<PokemonSpecies> species,
    List<PokemonStat> stats,
    List<PokemonType> types) {}

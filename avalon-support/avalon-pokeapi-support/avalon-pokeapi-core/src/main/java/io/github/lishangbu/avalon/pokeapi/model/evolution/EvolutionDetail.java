package io.github.lishangbu.avalon.pokeapi.model.evolution;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;

/// 宝可梦进化的详细条件信息
///
/// @param item                  进化所需的道具引用
/// @param trigger               触发进化的事件类型引用
/// @param gender                必需的性别 ID
/// @param heldItem              在触发进化事件期间必须持有的道具引用
/// @param knownMove             必须知道的招式引用
/// @param knownMoveType         必须知道的招式类型引用
/// @param location              必须触发进化的地点引用
/// @param minLevel              所需的最低等级
/// @param minHappiness          所需的最低幸福度
/// @param minBeauty             所需的最低美丽度
/// @param minAffection          所需的最低亲密度
/// @param needsOverworldRain    是否需要在主世界下雨
/// @param partySpecies          队伍中必须存在的宝可梦物种引用
/// @param partyType             队伍中必须存在的宝可梦类型引用
/// @param relativePhysicalStats 攻击与防御之间的相对关系（1/0/-1）
/// @param timeOfDay             所需的一天中的时间（如 day/night）
/// @param tradeSpecies          必须交换的宝可梦物种引用
/// @param turnUpsideDown        是否需要翻转设备
/// @author lishangbu
/// @see Item
/// @see EvolutionTrigger
/// @see Type
/// @since 2025/5/24
public record EvolutionDetail(
    NamedApiResource<Item> item,
    NamedApiResource<EvolutionTrigger> trigger,
    Integer gender,
    @JsonProperty("held_item") NamedApiResource<Item> heldItem,
    @JsonProperty("known_move") NamedApiResource<?> knownMove,
    @JsonProperty("known_move_type") NamedApiResource<Type> knownMoveType,
    NamedApiResource<?> location,
    @JsonProperty("min_level") Integer minLevel,
    @JsonProperty("min_happiness") Integer minHappiness,
    @JsonProperty("min_beauty") Integer minBeauty,
    @JsonProperty("min_affection") Integer minAffection,
    @JsonProperty("needs_overworld_rain") Boolean needsOverworldRain,
    @JsonProperty("party_species") NamedApiResource<?> partySpecies,
    @JsonProperty("party_type") NamedApiResource<Type> partyType,
    @JsonProperty("relative_physical_stats") Integer relativePhysicalStats,
    @JsonProperty("time_of_day") String timeOfDay,
    @JsonProperty("trade_species") NamedApiResource<?> tradeSpecies,
    @JsonProperty("turn_upside_down") Boolean turnUpsideDown) {}

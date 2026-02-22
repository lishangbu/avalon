package io.github.lishangbu.avalon.pokeapi.model.evolution;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import io.github.lishangbu.avalon.pokeapi.model.location.Location;
import io.github.lishangbu.avalon.pokeapi.model.location.Region;
import io.github.lishangbu.avalon.pokeapi.model.move.Move;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonSpecies;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;

/// 宝可梦进化的详细条件信息
///
/// @param item                  使宝可梦进化所需的道具
/// @param trigger               触发进化到此宝可梦物种的事件类型
/// @param gender                进化中的宝可梦物种的性别ID，必须为此值才能进化到此宝可梦物种
/// @param heldItem              进化中的宝可梦物种在进化触发事件期间必须持有的道具，才能进化到此宝可梦物种
/// @param knownMove             进化中的宝可梦物种在进化触发事件期间必须知道的招式，才能进化到此宝可梦物种
/// @param knownMoveType         进化中的宝可梦物种在进化触发事件期间必须知道此类型的招式，才能进化到此宝可梦物种
/// @param location              进化必须触发的位置
/// @param minLevel              进化中的宝可梦物种进化到此宝可梦物种所需的最低等级
/// @param minHappiness          进化中的宝可梦物种进化到此宝可梦物种所需的最低幸福度
/// @param minBeauty             进化中的宝可梦物种进化到此宝可梦物种所需的最低美丽度
/// @param minAffection          进化中的宝可梦物种进化到此宝可梦物种所需的最低亲密度
/// @param needsMultiplayer      是否需要多人联机游戏才能进化到此宝可梦物种（例如联合圈）
/// @param needsOverworldRain    是否必须在户外下雨才能使此宝可梦物种进化
/// @param partySpecies          玩家队伍中必须存在的宝可梦物种，以便进化中的宝可梦物种进化到此宝可梦物种
/// @param partyType             玩家在进化触发事件期间必须在队伍中有此类型的宝可梦，以便进化中的宝可梦物种进化到此宝可梦物种
/// @param relativePhysicalStats 宝可梦攻击和防御属性之间的必需关系。1表示攻击 > 防御。0表示攻击 = 防御。-1表示攻击 < 防御
/// @param timeOfDay             必需的一天中的时间。白天或夜晚
/// @param tradeSpecies          必须为此物种交易的宝可梦物种
/// @param turnUpsideDown        是否需要在宝可梦升级时将3DS上下颠倒
/// @param region                此进化可以发生的必需地区
/// @param baseForm              此进化可以发生的必需形态
/// @param usedMove              进化中的宝可梦物种在进化触发事件期间必须使用的招式，才能进化到此宝可梦物种
/// @param minMoveCount          招式必须使用的最少次数才能进化到此宝可梦物种
/// @param minSteps              必须行走的最少步数才能进化到此宝可梦物种
/// @param minDamageTaken        在进化触发事件期间必须承受的最少伤害量，才能进化到此宝可梦物种
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
    NamedApiResource<Location> location,
    @JsonProperty("min_level") Integer minLevel,
    @JsonProperty("min_happiness") Integer minHappiness,
    @JsonProperty("min_beauty") Integer minBeauty,
    @JsonProperty("min_affection") Integer minAffection,
    @JsonProperty("needs_multiplayer") Boolean needsMultiplayer,
    @JsonProperty("needs_overworld_rain") Boolean needsOverworldRain,
    @JsonProperty("party_species") NamedApiResource<PokemonSpecies> partySpecies,
    @JsonProperty("party_type") NamedApiResource<Type> partyType,
    @JsonProperty("relative_physical_stats") Integer relativePhysicalStats,
    @JsonProperty("time_of_day") String timeOfDay,
    @JsonProperty("trade_species") NamedApiResource<PokemonSpecies> tradeSpecies,
    @JsonProperty("turn_upside_down") Boolean turnUpsideDown,
    @JsonProperty("region") NamedApiResource<Region> region,
    @JsonProperty("base_form") NamedApiResource<PokemonSpecies> baseForm,
    @JsonProperty("used_move") NamedApiResource<Move> usedMove,
    @JsonProperty("min_move_count") Integer minMoveCount,
    @JsonProperty("min_steps") Integer minSteps,
    @JsonProperty("min_damage_taken") Integer minDamageTaken) {}

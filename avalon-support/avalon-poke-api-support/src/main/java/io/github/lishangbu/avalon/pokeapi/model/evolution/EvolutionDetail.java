package io.github.lishangbu.avalon.pokeapi.model.evolution;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;

/**
 * 宝可梦进化的详细条件信息
 *
 * @param item 使宝可梦进化所需的道具{@link Item}
 * @param trigger 触发进化为此宝可梦物种的事件类型{@link EvolutionTrigger}
 * @param gender 进化宝可梦物种必须具备的性别ID
 * @param heldItem 进化宝可梦物种在进化触发事件期间必须持有的道具{@link Item}
 * @param knownMove 进化宝可梦物种在进化触发事件期间必须知道的招式
 * @param knownMoveType 进化宝可梦物种在进化触发事件期间必须知道带有此类型{@link Type}的招式
 * @param location 必须在该位置触发进化
 * @param minLevel 进化宝可梦物种进化为此宝可梦物种所需的最低等级
 * @param minHappiness 进化宝可梦物种进化为此宝可梦物种所需的最低幸福度
 * @param minBeauty 进化宝可梦物种进化为此宝可梦物种所需的最低美丽度
 * @param minAffection 进化宝可梦物种进化为此宝可梦物种所需的最低亲密度
 * @param needsOverworldRain 是否需要在主世界下雨才能引起进化
 * @param partySpecies 玩家队伍中必须拥有的宝可梦物种，以便进化宝可梦物种进化为此宝可梦物种
 * @param partyType 玩家在进化触发事件中队伍中必须拥有的宝可梦类型{@link Type}，以便进化宝可梦物种进化为此宝可梦物种
 * @param relativePhysicalStats 宝可梦的攻击与防御状态之间的必要关系。1表示攻击大于防御，0表示攻击等于防御，-1表示攻击小于防御
 * @param timeOfDay 所需的一天中的时间。白天或夜晚
 * @param tradeSpecies 必须交换的宝可梦物种
 * @param turnUpsideDown 此宝可梦升级时是否需要将3DS倒置
 * @author lishangbu
 * @see Item
 * @see EvolutionTrigger
 * @see Type
 * @since 2025/5/24
 */
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

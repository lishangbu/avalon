package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFlavor;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 性格影响宝可梦的属性成长方式。更多详情请参见 <a href="http://bulbapedia.bulbagarden.net/wiki/Nature">Bulbapedia</a>。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param decreasedStat 具有此性格的宝可梦降低10%的属性{@link Stat}
 * @param increasedStat 具有此性格的宝可梦增加10%的属性{@link Stat}
 * @param hatesFlavor 具有此性格的宝可梦讨厌的口味{@link BerryFlavor}
 * @param likesFlavor 具有此性格的宝可梦喜欢的口味{@link BerryFlavor}
 * @param pokeathlonStatChanges 此性格影响的宝可梦竞技状态列表及其影响程度{@link NatureStatChange}
 * @param moveBattleStylePreferences 战斗风格列表及具有此性格的宝可梦在战斗宫殿或战斗帐篷中使用它们的可能性{@link
 *     MoveBattleStylePreference}
 * @param names 该资源在不同语言中列出的名称{@link Name}
 * @author lishangbu
 * @see Stat
 * @see BerryFlavor
 * @see NatureStatChange
 * @see MoveBattleStylePreference
 * @see Name
 * @since 2025/6/8
 */
public record Nature(
    Integer id,
    String name,
    @JsonProperty("decreased_stat") NamedApiResource<Stat> decreasedStat,
    @JsonProperty("increased_stat") NamedApiResource<Stat> increasedStat,
    @JsonProperty("hates_flavor") NamedApiResource<BerryFlavor> hatesFlavor,
    @JsonProperty("likes_flavor") NamedApiResource<BerryFlavor> likesFlavor,
    @JsonProperty("pokeathlon_stat_changes") List<NatureStatChange> pokeathlonStatChanges,
    @JsonProperty("move_battle_style_preferences")
        List<MoveBattleStylePreference> moveBattleStylePreferences,
    List<Name> names) {}

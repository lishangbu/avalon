package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFlavor;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 性格模型
///
/// 性格影响宝可梦的属性成长方式，详情参考 [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Nature)
///
/// @param id                         资源标识符
/// @param name                       资源名称
/// @param decreasedStat              降低 10% 的属性引用 {@link Stat}
/// @param increasedStat              增加 10% 的属性引用 {@link Stat}
/// @param hatesFlavor                讨厌的口味引用 {@link BerryFlavor}
/// @param likesFlavor                喜欢的口味引用 {@link BerryFlavor}
/// @param pokeathlonStatChanges      影响的竞技状态列表及影响程度 {@link NatureStatChange}
/// @param moveBattleStylePreferences 战斗风格列表及该性格的使用偏好 {@link MoveBattleStylePreference}
/// @param names                      多语言名称列表 {@link Name}
/// @author lishangbu
/// @see Stat
/// @see BerryFlavor
/// @see NatureStatChange
/// @see MoveBattleStylePreference
/// @see Name
/// @since 2025/6/8
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

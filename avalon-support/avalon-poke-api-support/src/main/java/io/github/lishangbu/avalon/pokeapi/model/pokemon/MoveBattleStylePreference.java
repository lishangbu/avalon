package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveBattleStyle;

/**
 * 宝可梦在不同战斗风格下使用特定技能的偏好
 *
 * @param lowHpPreference 当HP低于一半时，使用该技能的概率百分比
 * @param highHpPreference 当HP高于一半时，使用该技能的概率百分比
 * @param moveBattleStyle 技能战斗风格{@link MoveBattleStyle}
 * @author lishangbu
 * @see MoveBattleStyle
 * @since 2025/6/8
 */
public record MoveBattleStylePreference(
    @JsonProperty("low_hp_preference") Integer lowHpPreference,
    @JsonProperty("high_hp_preference") Integer highHpPreference,
    @JsonProperty("move_battle_style") NamedApiResource<MoveBattleStyle> moveBattleStyle) {}

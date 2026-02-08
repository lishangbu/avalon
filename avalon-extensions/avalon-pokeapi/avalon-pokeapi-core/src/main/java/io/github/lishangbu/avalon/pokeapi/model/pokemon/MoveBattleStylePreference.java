package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveBattleStyle;

/// 招式战斗风格偏好模型
///
/// 表示宝可梦在特定战斗风格下使用某技能的概率偏好
///
/// @param lowHpPreference  当 HP 低于一半时的使用概率（百分比）
/// @param highHpPreference 当 HP 高于一半时的使用概率（百分比）
/// @param moveBattleStyle  技能的战斗风格引用
/// @author lishangbu
/// @see MoveBattleStyle
/// @since 2025/6/8
public record MoveBattleStylePreference(
    @JsonProperty("low_hp_preference") Integer lowHpPreference,
    @JsonProperty("high_hp_preference") Integer highHpPreference,
    @JsonProperty("move_battle_style") NamedApiResource<MoveBattleStyle> moveBattleStyle) {}

package io.github.lishangbu.avalon.pokeapi.model.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 招式元数据模型
///
/// 包含招式的额外信息，如副作用、吸血、暴击率等
///
/// @param ailment       招式可能造成的异常状态引用
/// @param category      招式所属类别引用
/// @param minHits       最小命中次数（可为 null）
/// @param maxHits       最大命中次数（可为 null）
/// @param minTurns      最小持续回合数（可为 null）
/// @param maxTurns      最大持续回合数（可为 null）
/// @param drain         吸血或反作用伤害百分比
/// @param healing       恢复 HP 的百分比
/// @param critRate      暴击率加成
/// @param ailmentChance 导致异常状态的概率
/// @param flinchChance  导致畏缩的概率
/// @param statChance    导致能力变化的概率
/// @author lishangbu
/// @see MoveAilment
/// @see MoveCategory
/// @since 2025/6/7
public record MoveMetaData(
        NamedApiResource<MoveAilment> ailment,
        NamedApiResource<MoveCategory> category,
        @JsonProperty("min_hits") Integer minHits,
        @JsonProperty("max_hits") Integer maxHits,
        @JsonProperty("min_turns") Integer minTurns,
        @JsonProperty("max_turns") Integer maxTurns,
        Integer drain,
        Integer healing,
        @JsonProperty("crit_rate") Integer critRate,
        @JsonProperty("ailment_chance") Integer ailmentChance,
        @JsonProperty("flinch_chance") Integer flinchChance,
        @JsonProperty("stat_chance") Integer statChance) {}

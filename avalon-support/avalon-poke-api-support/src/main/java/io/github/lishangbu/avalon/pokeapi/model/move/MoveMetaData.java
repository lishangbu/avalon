package io.github.lishangbu.avalon.pokeapi.model.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 招式的元数据信息，包含额外的数据，如副作用和效果几率。
 *
 * @param ailment 此招式对目标造成的状态异常{@link MoveAilment}
 * @param category 此招式所属的类别{@link MoveCategory}，例如伤害或状态异常
 * @param minHits 此招式命中的最小次数。如果总是只命中一次，则为null
 * @param maxHits 此招式命中的最大次数。如果总是只命中一次，则为null
 * @param minTurns 此招式持续生效的最小回合数。如果总是只持续一回合，则为null
 * @param maxTurns 此招式持续生效的最大回合数。如果总是只持续一回合，则为null
 * @param drain HP吸取（如果为正）或反作用伤害（如果为负），以造成伤害的百分比表示
 * @param healing 攻击方宝可梦恢复的HP量，以其最大HP的百分比表示
 * @param critRate 暴击率加成
 * @param ailmentChance 此攻击导致状态异常的可能性
 * @param flinchChance 此攻击导致目标宝可梦畏缩的可能性
 * @param statChance 此攻击导致目标宝可梦能力值变化的可能性
 * @author lishangbu
 * @see MoveAilment
 * @see MoveCategory
 * @since 2025/6/7
 */
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

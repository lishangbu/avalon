package io.github.lishangbu.avalon.pokeapi.model.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.common.VerboseEffect;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;
import java.util.List;

/**
 * 招式在之前游戏版本中的数值统计。
 *
 * @param accuracy 此招式成功的概率百分比值
 * @param effectChance 此招式效果触发的概率百分比值
 * @param power 此招式的基础威力，如果没有基础威力则为0
 * @param pp 技能点数。此招式可以使用的次数
 * @param effectEntries 此招式在不同语言中列出的效果{@link VerboseEffect}
 * @param type 此招式的属性{@link Type}类型
 * @param versionGroup 这些招式统计值生效的版本组{@link VersionGroup}
 * @author lishangbu
 * @see VerboseEffect
 * @see Type
 * @see VersionGroup
 * @since 2025/6/7
 */
public record PastMoveStatValues(
    Integer accuracy,
    @JsonProperty("effect_chance") Integer effectChance,
    Integer power,
    Integer pp,
    @JsonProperty("effect_entries") List<VerboseEffect> effectEntries,
    NamedApiResource<Type> type,
    @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup) {}

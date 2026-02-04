package io.github.lishangbu.avalon.pokeapi.model.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.common.VerboseEffect;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;
import java.util.List;

/// 历史招式数值模型
///
/// 表示招式在以往版本中的数值与描述
///
/// @param accuracy      命中率（百分比）
/// @param effectChance  效果触发概率（百分比）
/// @param power         基础威力
/// @param pp            技能点数
/// @param effectEntries 多语言效果描述
/// @param type          招式属性类型引用
/// @param versionGroup  此数值生效的版本组引用
/// @author lishangbu
/// @see VerboseEffect
/// @see Type
/// @see VersionGroup
/// @since 2025/6/7
public record PastMoveStatValues(
    Integer accuracy,
    @JsonProperty("effect_chance") Integer effectChance,
    Integer power,
    Integer pp,
    @JsonProperty("effect_entries") List<VerboseEffect> effectEntries,
    NamedApiResource<Type> type,
    @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup) {}

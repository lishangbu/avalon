package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterConditionValue;
import java.util.List;

/// PokeAPI 遭遇记录模型
///
/// @param minLevel        最低等级
/// @param maxLevel        最高等级
/// @param conditionValues 触发该遭遇条件所需的条件值
/// @param chance          该遭遇发生的几率
/// @param method          触发该遭遇的方式
/// @author lishangbu
/// @since 2025/5/20
public record Encounter(
        @JsonProperty("min_level") Integer minLevel,
        @JsonProperty("max_level") Integer maxLevel,
        @JsonProperty("condition_values")
                List<NamedApiResource<EncounterConditionValue>> conditionValues,
        Integer chance,
        NamedApiResource<?> method) {}

package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/Encounter</a>
 *
 * @param minLevel 该遇到的最低等级
 * @param maxLevel 该遇到的最高等级
 * @param conditionValues 触发该遇到条件所需的条件值
 * @param chance 该遇到发生的几率
 * @param method 触发该遇到的方式
 * @author lishangbu
 * @since 2025/5/20
 */
public record Encounter(
    @JsonProperty("min_level") Integer minLevel,
    @JsonProperty("max_level") Integer maxLevel,
    @JsonProperty("condition_values") NamedApiResource<?> conditionValues,
    Integer chance,
    NamedApiResource<?> method) {}

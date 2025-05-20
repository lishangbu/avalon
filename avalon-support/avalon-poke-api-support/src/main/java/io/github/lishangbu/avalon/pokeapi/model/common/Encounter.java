package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/Encounter</a>
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record Encounter(
    @JsonProperty("min_level") Integer minLevel,
    @JsonProperty("max_level") Integer maxLevel,
    @JsonProperty("condition_values") NamedApiResource<?> conditionValues,
    Integer chance,
    NamedApiResource<?> method) {
  /** 获取该遇到的最低等级 */
  public Integer minLevel() {
    return minLevel;
  }

  /** 获取该遇到的最高等级 */
  public Integer maxLevel() {
    return maxLevel;
  }

  /** 获取触发该遇到条件所需的条件值 */
  public NamedApiResource<?> conditionValues() {
    return conditionValues;
  }

  /** 获取该遇到发生的几率 */
  public Integer chance() {
    return chance;
  }

  /** 获取触发该遇到的方式 */
  public NamedApiResource<?> method() {
    return method;
  }
}

package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/VersionEncounterDetail</a>
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record VersionEncounterDetail(
    NamedApiResource<?> version,
    @JsonProperty("max_chance") Integer maxChance,
    @JsonProperty("encounter_details") List<Encounter> encounter_details) {
  /** 获取该遭遇发生的游戏版本 */
  public NamedApiResource<?> version() {
    return version;
  }

  /** 获取所有遭遇潜力的最大百分比 */
  public Integer maxChance() {
    return maxChance;
  }

  /** 获取遭遇及其详细信息的列表 */
  public List<Encounter> encounter_details() {
    return encounter_details;
  }
}

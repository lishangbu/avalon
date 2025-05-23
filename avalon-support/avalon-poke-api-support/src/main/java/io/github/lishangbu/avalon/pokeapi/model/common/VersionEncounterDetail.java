package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/VersionEncounterDetail</a>
 *
 * @param version 该遭遇发生的游戏版本
 * @param maxChance 所有遭遇潜力的最大百分比
 * @param encounter_details 遭遇及其详细信息的列表
 * @author lishangbu
 * @since 2025/5/20
 */
public record VersionEncounterDetail(
    NamedApiResource<?> version,
    @JsonProperty("max_chance") Integer maxChance,
    @JsonProperty("encounter_details") List<Encounter> encounter_details) {}

package io.github.lishangbu.avalon.content.creature.model;

import java.util.Map;

/** 物种七维成长内容，生产加载与离线重放共用唯一记录格式。 */
public record SpeciesStatsDefinition(Map<String, SpeciesStatsStats> species) {
}

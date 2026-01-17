package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.location.PalParkArea;

/// 合作公园遭遇区域模型
///
/// 表示在伙伴公园中遇到特定宝可梦的基本信息
///
/// @param baseScore 捕获时获得的基础分数
/// @param rate      遇到该宝可梦的概率
/// @param area      伙伴公园区域引用
/// @author lishangbu
/// @see PalParkArea
/// @since 2025/6/8
public record PalParkEncounterArea(
    @JsonProperty("base_score") Integer baseScore,
    Integer rate,
    NamedApiResource<PalParkArea> area) {}

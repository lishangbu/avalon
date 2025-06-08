package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.location.PalParkArea;

/**
 * 宝可梦伙伴公园遭遇区域
 *
 * @param baseScore 当玩家在伙伴公园运行期间捕获所引用的宝可梦时获得的基础分数
 * @param rate 在此伙伴公园区域遇到所引用宝可梦的基础概率
 * @param area 发生此遭遇的伙伴公园区域{@link PalParkArea}
 * @author lishangbu
 * @see PalParkArea
 * @since 2025/6/8
 */
public record PalParkEncounterArea(
    @JsonProperty("base_score") Integer baseScore,
    Integer rate,
    NamedApiResource<PalParkArea> area) {}

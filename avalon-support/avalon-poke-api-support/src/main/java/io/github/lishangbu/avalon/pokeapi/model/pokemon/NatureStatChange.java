package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 性格对宝可梦竞技状态的改变
 *
 * @param maxChange 改变的数值
 * @param pokeathlonStat 受影响的竞技状态{@link PokeathlonStat}
 * @author lishangbu
 * @since 2025/6/8
 * @see PokeathlonStat
 */
public record NatureStatChange(
    @JsonProperty("max_change") Integer maxChange,
    @JsonProperty("pokeathlon_stat") NamedApiResource<PokeathlonStat> pokeathlonStat) {}

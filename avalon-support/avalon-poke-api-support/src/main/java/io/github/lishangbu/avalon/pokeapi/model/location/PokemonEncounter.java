package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.common.VersionEncounterDetail;
import java.util.List;

/**
 * PokemonEncounter
 *
 * @param pokemon 被遇到的宝可梦
 * @param versionDetails 可能在引用位置区域中出现的宝可梦的版本和遭遇列表
 * @author lishangbu
 * @since 2025/5/26
 */
public record PokemonEncounter(
    NamedApiResource<?> pokemon,
    @JsonProperty("version_details") List<VersionEncounterDetail> versionDetails) {}

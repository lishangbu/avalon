package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterMethod;
import java.util.List;

/**
 * EncounterMethodRate
 *
 * @param encounterMethod 宝可梦在区域中可能遇到的方式
 * @param versionDetails 在游戏版本中遇到的几率
 * @author lishangbu
 * @since 2025/5/26
 */
public record EncounterMethodRate(
    @JsonProperty("encounter_method") NamedApiResource<EncounterMethod> encounterMethod,
    @JsonProperty("version_details") List<EncounterVersionDetails> versionDetails) {}

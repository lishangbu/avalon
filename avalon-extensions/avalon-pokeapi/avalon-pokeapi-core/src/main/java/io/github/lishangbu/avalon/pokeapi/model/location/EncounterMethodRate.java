package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterMethod;
import java.util.List;

/// 遭遇方式与对应版本几率
///
/// @param encounterMethod 宝可梦在区域中可能遇到的方式引用
/// @param versionDetails  在不同版本中遇到的几率详情列表
/// @author lishangbu
/// @since 2025/5/26
public record EncounterMethodRate(
        @JsonProperty("encounter_method") NamedApiResource<EncounterMethod> encounterMethod,
        @JsonProperty("version_details") List<EncounterVersionDetails> versionDetails) {}

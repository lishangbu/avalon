package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.common.VersionEncounterDetail;
import java.util.List;

/// Pokemon 遭遇模型
///
/// @param pokemon        被遇到的宝可梦引用
/// @param versionDetails 在不同版本中该宝可梦的遭遇详情列表
/// @author lishangbu
/// @see VersionEncounterDetail
/// @since 2025/5/26
public record PokemonEncounter(
    NamedApiResource<?> pokemon,
    @JsonProperty("version_details") List<VersionEncounterDetail> versionDetails) {}

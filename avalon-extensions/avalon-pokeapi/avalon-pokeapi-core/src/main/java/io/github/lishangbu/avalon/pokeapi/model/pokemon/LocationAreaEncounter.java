package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.common.VersionEncounterDetail;
import io.github.lishangbu.avalon.pokeapi.model.location.LocationArea;
import java.util.List;

/// 地点区域遭遇模型
///
/// 表示在特定地点区域中可能遭遇到的宝可梦及对应版本的遭遇详情
///
/// @param locationArea   地点区域引用
/// @param versionDetails 版本与遭遇详情列表
/// @author lishangbu
/// @see LocationArea
/// @see VersionEncounterDetail
/// @since 2025/6/8
public record LocationAreaEncounter(
        @JsonProperty("location_area") NamedApiResource<LocationArea> locationArea,
        @JsonProperty("version_details") List<VersionEncounterDetail> versionDetails) {}

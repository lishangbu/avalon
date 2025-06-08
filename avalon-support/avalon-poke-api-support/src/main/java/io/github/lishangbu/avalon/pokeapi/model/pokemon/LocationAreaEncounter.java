package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.common.VersionEncounterDetail;
import io.github.lishangbu.avalon.pokeapi.model.location.LocationArea;
import java.util.List;

/**
 * 宝可梦地点区域是可以找到宝可梦的区域。
 *
 * @param locationArea 可以遇到所引用宝可梦的地点区域{@link LocationArea}
 * @param versionDetails 可能发生与所引用宝可梦遭遇的版本和遭遇{@link VersionEncounterDetail}列表
 * @author lishangbu
 * @see LocationArea
 * @see VersionEncounterDetail
 * @since 2025/6/8
 */
public record LocationAreaEncounter(
    @JsonProperty("location_area") NamedApiResource<LocationArea> locationArea,
    @JsonProperty("version_details") List<VersionEncounterDetail> versionDetails) {}

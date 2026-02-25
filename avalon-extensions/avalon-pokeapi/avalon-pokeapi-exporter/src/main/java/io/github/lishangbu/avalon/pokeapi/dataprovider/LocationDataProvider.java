package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.LocationExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.location.Location;
import io.github.lishangbu.avalon.pokeapi.model.location.Region;
import org.springframework.stereotype.Service;

/// 位置数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class LocationDataProvider extends AbstractPokeApiDataProvider<Location, LocationExcelDTO> {

    @Override
    public LocationExcelDTO convert(Location location) {
        LocationExcelDTO result = new LocationExcelDTO();
        result.setId(location.id());
        result.setInternalName(location.name());
        result.setName(resolveLocalizedName(location.names(), location.name()));
        NamedApiResource<Region> regionNamedApiResource = location.region();
        if (regionNamedApiResource != null) {
            result.setRegionName(regionNamedApiResource.name());
        }
        return result;
    }
}

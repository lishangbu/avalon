package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.LocationAreaExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.location.Location;
import io.github.lishangbu.avalon.pokeapi.model.location.LocationArea;
import org.springframework.stereotype.Service;

/// 位置区域数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class LocationAreaDataProvider
    extends AbstractPokeApiDataProvider<LocationArea, LocationAreaExcelDTO> {

  @Override
  public LocationAreaExcelDTO convert(LocationArea locationArea) {
    LocationAreaExcelDTO result = new LocationAreaExcelDTO();
    result.setId(locationArea.id());
    result.setInternalName(locationArea.name());
    result.setName(resolveLocalizedName(locationArea.names(), locationArea.name()));
    result.setGameIndex(locationArea.gameIndex());
    NamedApiResource<Location> locationNamedApiResource = locationArea.location();
    if (locationNamedApiResource != null) {
      result.setLocationName(locationNamedApiResource.name());
    }
    return result;
  }
}

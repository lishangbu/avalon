package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.RegionExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import io.github.lishangbu.avalon.pokeapi.model.location.Region;
import org.springframework.stereotype.Service;

/// 地区数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class RegionDataProvider extends AbstractPokeApiDataProvider<Region, RegionExcelDTO> {

  @Override
  public RegionExcelDTO convert(Region region) {
    RegionExcelDTO result = new RegionExcelDTO();
    result.setId(region.id());
    result.setInternalName(region.name());
    result.setName(resolveLocalizedName(region.names(), region.name()));
    NamedApiResource<Generation> generationNamedApiResource = region.mainGeneration();
    if (generationNamedApiResource != null) {
      result.setMainGenerationName(generationNamedApiResource.name());
    }
    return result;
  }
}

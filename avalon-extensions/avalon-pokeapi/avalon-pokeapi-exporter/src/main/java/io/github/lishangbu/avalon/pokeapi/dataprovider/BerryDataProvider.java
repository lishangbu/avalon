package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.BerryExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.berry.Berry;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.stereotype.Service;

/// 树果数据提供者
///
/// @author lishangbu
/// @since 2026/2/4
@Service
public class BerryDataProvider extends AbstractPokeApiDataProvider<Berry, BerryExcelDTO> {

  @Override
  public BerryExcelDTO convert(Berry berry) {
    BerryExcelDTO result = new BerryExcelDTO();
    result.setId(berry.id());
    result.setInternalName(berry.name());
    Item item =
        pokeApiService.getEntityFromUri(
            PokeDataTypeEnum.ITEM, NamedApiResourceUtils.getId(berry.item()));
    result.setName(resolveLocalizedName(item.names(), item.name()));
    result.setGrowthTime(berry.growthTime());
    result.setMaxHarvest(berry.maxHarvest());
    result.setBulk(berry.size());
    result.setSmoothness(berry.smoothness());
    result.setSoilDryness(berry.soilDryness());
    result.setBerryFirmnessId(NamedApiResourceUtils.getId(berry.firmness()));
    result.setNaturalGiftTypeId(NamedApiResourceUtils.getId(berry.naturalGiftType()));
    result.setNaturalGiftPower(berry.naturalGiftPower());
    return result;
  }
}

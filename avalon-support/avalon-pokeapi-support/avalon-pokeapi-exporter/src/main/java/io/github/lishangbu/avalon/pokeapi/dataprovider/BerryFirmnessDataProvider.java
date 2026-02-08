package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.BerryFirmnessExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFirmness;
import org.springframework.stereotype.Service;

/// 树果硬度数据提供者
///
/// @author lishangbu
/// @since 2026/2/4
@Service
public class BerryFirmnessDataProvider
    extends AbstractPokeApiDataProvider<BerryFirmness, BerryFirmnessExcelDTO> {

  @Override
  public BerryFirmnessExcelDTO convert(BerryFirmness berryFirmness) {
    BerryFirmnessExcelDTO result = new BerryFirmnessExcelDTO();
    result.setId(berryFirmness.id());
    result.setInternalName(berryFirmness.name());
    result.setName(resolveLocalizedNameFromNames(berryFirmness.names(), berryFirmness.name()));
    return result;
  }
}

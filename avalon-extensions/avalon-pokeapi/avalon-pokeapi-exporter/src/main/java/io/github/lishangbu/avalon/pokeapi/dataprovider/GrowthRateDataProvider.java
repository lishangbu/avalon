package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.GrowthRateExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.GrowthRate;
import org.springframework.stereotype.Service;

/// 成长速率数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class GrowthRateDataProvider
    extends AbstractPokeApiDataProvider<GrowthRate, GrowthRateExcelDTO> {

  @Override
  public GrowthRateExcelDTO convert(GrowthRate growthRate) {
    GrowthRateExcelDTO result = new GrowthRateExcelDTO();
    result.setId(growthRate.id());
    result.setInternalName(growthRate.name());
    result.setName(resolveLocalizedDescription(growthRate.descriptions()));
    result.setFormula(growthRate.formula());
    result.setDescription(resolveLocalizedDescription(growthRate.descriptions()));
    return result;
  }
}

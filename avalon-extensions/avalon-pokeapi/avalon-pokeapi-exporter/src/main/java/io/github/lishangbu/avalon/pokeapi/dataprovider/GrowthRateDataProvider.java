package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.GrowthRateExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.GrowthRate;
import java.util.Map;
import org.springframework.stereotype.Service;

/// 成长速率数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class GrowthRateDataProvider
    extends AbstractPokeApiDataProvider<GrowthRate, GrowthRateExcelDTO> {

  private final Map<String, String> growthRateNameCache =
      Map.of(
          "slow", "慢",
          "medium", "较快",
          "fast", "快",
          "medium-slow", "较慢",
          "slow-then-very-fast", "最快",
          "fast-then-very-slow", "最慢");

  @Override
  public GrowthRateExcelDTO convert(GrowthRate growthRate) {
    GrowthRateExcelDTO result = new GrowthRateExcelDTO();
    result.setId(growthRate.id());
    result.setInternalName(growthRate.name());
    result.setName(growthRateNameCache.getOrDefault(growthRate.name(), growthRate.name()));
    result.setFormula(growthRate.formula());
    result.setDescription(resolveLocalizedDescription(growthRate.descriptions()));
    return result;
  }
}

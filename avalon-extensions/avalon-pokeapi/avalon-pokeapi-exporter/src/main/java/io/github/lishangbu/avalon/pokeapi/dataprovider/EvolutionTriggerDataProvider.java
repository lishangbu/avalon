package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.EvolutionTriggerExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.evolution.EvolutionTrigger;
import org.springframework.stereotype.Service;

/// 进化触发器数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class EvolutionTriggerDataProvider
    extends AbstractPokeApiDataProvider<EvolutionTrigger, EvolutionTriggerExcelDTO> {

  @Override
  public EvolutionTriggerExcelDTO convert(EvolutionTrigger evolutionTrigger) {
    EvolutionTriggerExcelDTO result = new EvolutionTriggerExcelDTO();
    result.setId(evolutionTrigger.id());
    result.setInternalName(evolutionTrigger.name());
    result.setName(resolveLocalizedName(evolutionTrigger.names(), evolutionTrigger.name()));
    return result;
  }
}

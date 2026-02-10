package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.EvolutionChainExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.evolution.EvolutionChain;
import org.springframework.stereotype.Service;

/// 进化链数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class EvolutionChainDataProvider
    extends AbstractPokeApiDataProvider<EvolutionChain, EvolutionChainExcelDTO> {

  @Override
  public EvolutionChainExcelDTO convert(EvolutionChain evolutionChain) {
    EvolutionChainExcelDTO result = new EvolutionChainExcelDTO();
    result.setId(evolutionChain.id());
    result.setBabyTriggerItemName(
        evolutionChain.babyTriggerItem() != null ? evolutionChain.babyTriggerItem().name() : null);
    result.setChainSpeciesName(evolutionChain.chain().species().name());
    return result;
  }
}

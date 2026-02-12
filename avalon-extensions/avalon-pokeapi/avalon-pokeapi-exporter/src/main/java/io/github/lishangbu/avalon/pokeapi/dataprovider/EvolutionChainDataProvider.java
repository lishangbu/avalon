package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.EvolutionChainExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.evolution.ChainLink;
import io.github.lishangbu.avalon.pokeapi.model.evolution.EvolutionChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// 进化链数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
@Slf4j
public class EvolutionChainDataProvider
    extends AbstractPokeApiDataProvider<EvolutionChain, EvolutionChainExcelDTO> {

  @Override
  public EvolutionChainExcelDTO convert(EvolutionChain evolutionChain) {
    EvolutionChainExcelDTO result = new EvolutionChainExcelDTO();
    result.setId(evolutionChain.id());
    result.setBabyTriggerItemName(
        evolutionChain.babyTriggerItem() != null ? evolutionChain.babyTriggerItem().name() : null);
    ChainLink chain = evolutionChain.chain();
    result.setChainSpeciesName(chain.species().name());
    if(chain.evolutionDetails() != null&&!chain.evolutionDetails().isEmpty()){
      log.info("Processing evolution details for chain ID [{}]: [{}]", evolutionChain.id(), chain.evolutionDetails());
    }
    return result;
  }
}

package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.EncounterConditionExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterCondition;
import org.springframework.stereotype.Service;

/// 遭遇条件数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class EncounterConditionDataProvider
    extends AbstractPokeApiDataProvider<EncounterCondition, EncounterConditionExcelDTO> {

  @Override
  public EncounterConditionExcelDTO convert(EncounterCondition encounterCondition) {
    EncounterConditionExcelDTO result = new EncounterConditionExcelDTO();
    result.setId(encounterCondition.id());
    result.setInternalName(encounterCondition.name());
    result.setName(resolveLocalizedName(encounterCondition.names(), encounterCondition.name()));
    return result;
  }
}

package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.EncounterConditionValueExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterConditionValue;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.stereotype.Service;

/// 遭遇条件值数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class EncounterConditionValueDataProvider
    extends AbstractPokeApiDataProvider<EncounterConditionValue, EncounterConditionValueExcelDTO> {

  @Override
  public EncounterConditionValueExcelDTO convert(EncounterConditionValue encounterConditionValue) {
    EncounterConditionValueExcelDTO result = new EncounterConditionValueExcelDTO();
    result.setId(encounterConditionValue.id());
    result.setInternalName(encounterConditionValue.name());
    result.setName(
        resolveLocalizedName(encounterConditionValue.names(), encounterConditionValue.name()));
    result.setEncounterConditionId(
        NamedApiResourceUtils.getId(encounterConditionValue.condition()));
    return result;
  }
}

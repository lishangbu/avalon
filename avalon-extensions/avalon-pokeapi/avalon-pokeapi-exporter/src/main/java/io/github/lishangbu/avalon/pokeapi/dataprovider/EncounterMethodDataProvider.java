package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.EncounterMethodExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterMethod;
import org.springframework.stereotype.Service;

/// 遭遇方式数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class EncounterMethodDataProvider
        extends AbstractPokeApiDataProvider<EncounterMethod, EncounterMethodExcelDTO> {

    @Override
    public EncounterMethodExcelDTO convert(EncounterMethod encounterMethod) {
        EncounterMethodExcelDTO result = new EncounterMethodExcelDTO();
        result.setId(encounterMethod.id());
        result.setInternalName(encounterMethod.name());
        result.setName(resolveLocalizedName(encounterMethod.names(), encounterMethod.name()));
        result.setSortingOrder(encounterMethod.order());
        return result;
    }
}

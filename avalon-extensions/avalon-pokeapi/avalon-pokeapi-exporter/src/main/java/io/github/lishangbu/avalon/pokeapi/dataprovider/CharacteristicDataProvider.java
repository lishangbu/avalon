package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.CharacteristicExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Characteristic;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.stereotype.Service;

/// 特征数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class CharacteristicDataProvider
        extends AbstractPokeApiDataProvider<Characteristic, CharacteristicExcelDTO> {

    @Override
    public CharacteristicExcelDTO convert(Characteristic characteristic) {
        CharacteristicExcelDTO result = new CharacteristicExcelDTO();
        result.setId(characteristic.id());
        result.setGeneModulo(characteristic.geneModulo());
        result.setPossibleValues(characteristic.possibleValues().toString());
        result.setHighestStatId(NamedApiResourceUtils.getId(characteristic.highestStat()));
        result.setDescription(resolveLocalizedDescription(characteristic.descriptions()));
        return result;
    }
}

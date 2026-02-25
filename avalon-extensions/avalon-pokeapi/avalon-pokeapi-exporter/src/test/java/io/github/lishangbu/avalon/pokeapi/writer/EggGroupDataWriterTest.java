package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.EggGroupExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.EggGroup;
import jakarta.annotation.Resource;

class EggGroupDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<EggGroup> eggGroupDataProvider;

    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.EGG_GROUP;
    }

    @Override
    Class<EggGroupExcelDTO> getExcelClass() {
        return EggGroupExcelDTO.class;
    }

    @Override
    PokeApiDataProvider<EggGroup> getDataProvider() {
        return eggGroupDataProvider;
    }
}

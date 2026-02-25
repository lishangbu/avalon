package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.GenderExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Gender;
import jakarta.annotation.Resource;

class GenderDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<Gender> genderDataProvider;

    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.GENDER;
    }

    @Override
    Class<GenderExcelDTO> getExcelClass() {
        return GenderExcelDTO.class;
    }

    @Override
    PokeApiDataProvider<Gender> getDataProvider() {
        return genderDataProvider;
    }
}

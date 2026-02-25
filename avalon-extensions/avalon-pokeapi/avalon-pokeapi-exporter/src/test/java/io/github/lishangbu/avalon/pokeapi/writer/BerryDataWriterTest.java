package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.BerryExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.berry.Berry;
import jakarta.annotation.Resource;

class BerryDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<Berry> berryDataProvider;

    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.BERRY;
    }

    @Override
    Class<BerryExcelDTO> getExcelClass() {
        return BerryExcelDTO.class;
    }

    @Override
    PokeApiDataProvider<Berry> getDataProvider() {
        return berryDataProvider;
    }
}

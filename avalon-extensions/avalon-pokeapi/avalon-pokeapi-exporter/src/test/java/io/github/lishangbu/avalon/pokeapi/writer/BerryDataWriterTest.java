package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.BerryDataProvider;
import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.BerryExcelDTO;
import org.springframework.beans.factory.annotation.Autowired;

class BerryDataWriterTest extends AbstractExcelWriterTest {

  @Autowired private BerryDataProvider berryDataProvider;

  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.BERRY;
  }

  @Override
  Class getExcelClass() {
    return BerryExcelDTO.class;
  }

  @Override
  PokeApiDataProvider getDataProvider() {
    return berryDataProvider;
  }
}

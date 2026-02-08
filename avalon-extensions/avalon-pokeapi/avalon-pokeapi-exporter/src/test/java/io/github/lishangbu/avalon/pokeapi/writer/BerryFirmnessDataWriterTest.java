package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.BerryFirmnessDataProvider;
import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.BerryFirmnessExcelDTO;
import org.springframework.beans.factory.annotation.Autowired;

class BerryFirmnessDataWriterTest extends AbstractExcelWriterTest {

  @Autowired private BerryFirmnessDataProvider berryFirmnessDataProvider;

  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.BERRY_FIRMNESS;
  }

  @Override
  Class getExcelClass() {
    return BerryFirmnessExcelDTO.class;
  }

  @Override
  PokeApiDataProvider getDataProvider() {
    return berryFirmnessDataProvider;
  }
}

package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.BerryFirmnessExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFirmness;
import jakarta.annotation.Resource;

class BerryFirmnessDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<BerryFirmness> berryFirmnessDataProvider;

  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.BERRY_FIRMNESS;
  }

  @Override
  Class<BerryFirmnessExcelDTO> getExcelClass() {
    return BerryFirmnessExcelDTO.class;
  }

  @Override
  PokeApiDataProvider<BerryFirmness> getDataProvider() {
    return berryFirmnessDataProvider;
  }
}

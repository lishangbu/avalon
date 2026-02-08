package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.BerryFlavorExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFlavor;
import jakarta.annotation.Resource;

class BerryFlavorDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<BerryFlavor> berryFlavorDataProvider;

  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.BERRY_FLAVOR;
  }

  @Override
  Class<BerryFlavorExcelDTO> getExcelClass() {
    return BerryFlavorExcelDTO.class;
  }

  @Override
  PokeApiDataProvider<BerryFlavor> getDataProvider() {
    return berryFlavorDataProvider;
  }
}

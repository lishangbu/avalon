package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.BerryFlavorDataProvider;
import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.BerryFlavorExcelDTO;
import org.springframework.beans.factory.annotation.Autowired;

class BerryFlavorDataWriterTest extends AbstractExcelWriterTest {

  @Autowired private BerryFlavorDataProvider berryFlavorDataProvider;

  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.BERRY_FLAVOR;
  }

  @Override
  Class getExcelClass() {
    return BerryFlavorExcelDTO.class;
  }

  @Override
  PokeApiDataProvider getDataProvider() {
    return berryFlavorDataProvider;
  }
}

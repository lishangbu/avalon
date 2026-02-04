package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.dataprovider.TypeDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.TypeExcelDTO;
import org.springframework.beans.factory.annotation.Autowired;

class TypeDataWriterTest extends AbstractExcelWriterTest {

  @Autowired private TypeDataProvider typeDataProvider;

  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.TYPE;
  }

  @Override
  Class getExcelClass() {
    return TypeExcelDTO.class;
  }

  @Override
  PokeApiDataProvider getDataProvider() {
    return typeDataProvider;
  }
}

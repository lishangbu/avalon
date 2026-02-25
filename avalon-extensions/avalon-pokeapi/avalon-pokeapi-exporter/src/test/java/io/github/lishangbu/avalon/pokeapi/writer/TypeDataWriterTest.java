package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.TypeExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;
import jakarta.annotation.Resource;

class TypeDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<Type> typeDataProvider;

    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.TYPE;
    }

    @Override
    Class<TypeExcelDTO> getExcelClass() {
        return TypeExcelDTO.class;
    }

    @Override
    PokeApiDataProvider<Type> getDataProvider() {
        return typeDataProvider;
    }
}

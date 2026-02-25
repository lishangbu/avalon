package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.PokemonSpeciesExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonSpecies;
import jakarta.annotation.Resource;

/// 宝可梦种类数据写入器测试
///
/// 测试 PokemonSpeciesDataWriter 的功能，包括数据获取和Excel写入
class PokemonSpeciesDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<PokemonSpecies> pokemonSpeciesDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return POKEMON_SPECIES 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.POKEMON_SPECIES;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return PokemonSpeciesExcelDTO.class
    @Override
    Class<PokemonSpeciesExcelDTO> getExcelClass() {
        return PokemonSpeciesExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 宝可梦种类数据提供者实例
    @Override
    PokeApiDataProvider<PokemonSpecies> getDataProvider() {
        return pokemonSpeciesDataProvider;
    }
}

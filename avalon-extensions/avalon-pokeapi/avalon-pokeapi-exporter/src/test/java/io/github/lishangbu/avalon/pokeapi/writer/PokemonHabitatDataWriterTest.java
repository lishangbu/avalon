package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.PokemonHabitatExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonHabitat;
import jakarta.annotation.Resource;

/// 宝可梦栖息地数据写入器测试
///
/// 测试 PokemonHabitatDataWriter 的功能，包括数据获取和Excel写入
class PokemonHabitatDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<PokemonHabitat> pokemonHabitatDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return POKEMON_HABITAT 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.POKEMON_HABITAT;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return PokemonHabitatExcelDTO.class
    @Override
    Class<PokemonHabitatExcelDTO> getExcelClass() {
        return PokemonHabitatExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 宝可梦栖息地数据提供者实例
    @Override
    PokeApiDataProvider<PokemonHabitat> getDataProvider() {
        return pokemonHabitatDataProvider;
    }
}

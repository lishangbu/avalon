package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.PokemonShapeExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonShape;
import jakarta.annotation.Resource;

/// 宝可梦形状数据写入器测试
///
/// 测试 PokemonShapeDataWriter 的功能，包括数据获取和Excel写入
class PokemonShapeDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<PokemonShape> pokemonShapeDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return POKEMON_SHAPE 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.POKEMON_SHAPE;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return PokemonShapeExcelDTO.class
    @Override
    Class<PokemonShapeExcelDTO> getExcelClass() {
        return PokemonShapeExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 宝可梦形状数据提供者实例
    @Override
    PokeApiDataProvider<PokemonShape> getDataProvider() {
        return pokemonShapeDataProvider;
    }
}

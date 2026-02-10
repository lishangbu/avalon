package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.PokemonColorExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonColor;
import jakarta.annotation.Resource;

/// 宝可梦颜色数据写入器测试
///
/// 测试 PokemonColorDataWriter 的功能，包括数据获取和Excel写入
class PokemonColorDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<PokemonColor> pokemonColorDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return POKEMON_COLOR 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.POKEMON_COLOR;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return PokemonColorExcelDTO.class
  @Override
  Class<PokemonColorExcelDTO> getExcelClass() {
    return PokemonColorExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 宝可梦颜色数据提供者实例
  @Override
  PokeApiDataProvider<PokemonColor> getDataProvider() {
    return pokemonColorDataProvider;
  }
}

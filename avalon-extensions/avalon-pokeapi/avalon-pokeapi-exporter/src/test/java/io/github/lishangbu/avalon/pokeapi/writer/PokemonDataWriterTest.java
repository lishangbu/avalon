package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.PokemonExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Pokemon;
import jakarta.annotation.Resource;

/// 宝可梦数据写入器测试
///
/// 测试 PokemonDataWriter 的功能，包括数据获取和Excel写入
class PokemonDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<Pokemon> pokemonDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return POKEMON 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.POKEMON;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return PokemonExcelDTO.class
  @Override
  Class<PokemonExcelDTO> getExcelClass() {
    return PokemonExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 宝可梦数据提供者实例
  @Override
  PokeApiDataProvider<Pokemon> getDataProvider() {
    return pokemonDataProvider;
  }
}

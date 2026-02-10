package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.PokemonFormExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonForm;
import jakarta.annotation.Resource;

/// 宝可梦形态数据写入器测试
///
/// 测试 PokemonFormDataWriter 的功能，包括数据获取和Excel写入
class PokemonFormDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<PokemonForm> pokemonFormDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return POKEMON_FORM 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.POKEMON_FORM;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return PokemonFormExcelDTO.class
  @Override
  Class<PokemonFormExcelDTO> getExcelClass() {
    return PokemonFormExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 宝可梦形态数据提供者实例
  @Override
  PokeApiDataProvider<PokemonForm> getDataProvider() {
    return pokemonFormDataProvider;
  }
}

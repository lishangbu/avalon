package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.CharacteristicExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Characteristic;
import jakarta.annotation.Resource;

/// 特征数据写入器测试
///
/// 测试 CharacteristicDataWriter 的功能，包括数据获取和Excel写入
class CharacteristicDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<Characteristic> characteristicDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return CHARACTERISTIC 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.CHARACTERISTIC;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return CharacteristicExcelDTO.class
  @Override
  Class<CharacteristicExcelDTO> getExcelClass() {
    return CharacteristicExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 特征数据提供者实例
  @Override
  PokeApiDataProvider<Characteristic> getDataProvider() {
    return characteristicDataProvider;
  }
}

package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.GrowthRateExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.GrowthRate;
import jakarta.annotation.Resource;

/// 成长速率数据写入器测试
///
/// 测试 GrowthRateDataWriter 的功能，包括数据获取和Excel写入
class GrowthRateDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<GrowthRate> growthRateDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return GROWTH_RATE 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.GROWTH_RATE;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return GrowthRateExcelDTO.class
  @Override
  Class<GrowthRateExcelDTO> getExcelClass() {
    return GrowthRateExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 成长速率数据提供者实例
  @Override
  PokeApiDataProvider<GrowthRate> getDataProvider() {
    return growthRateDataProvider;
  }
}

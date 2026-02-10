package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.EvolutionTriggerExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.evolution.EvolutionTrigger;
import jakarta.annotation.Resource;

/// 进化触发器数据写入器测试
///
/// 测试 EvolutionTriggerDataWriter 的功能，包括数据获取和Excel写入
class EvolutionTriggerDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<EvolutionTrigger> evolutionTriggerDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return EVOLUTION_TRIGGER 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.EVOLUTION_TRIGGER;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return EvolutionTriggerExcelDTO.class
  @Override
  Class<EvolutionTriggerExcelDTO> getExcelClass() {
    return EvolutionTriggerExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 进化触发器数据提供者实例
  @Override
  PokeApiDataProvider<EvolutionTrigger> getDataProvider() {
    return evolutionTriggerDataProvider;
  }
}

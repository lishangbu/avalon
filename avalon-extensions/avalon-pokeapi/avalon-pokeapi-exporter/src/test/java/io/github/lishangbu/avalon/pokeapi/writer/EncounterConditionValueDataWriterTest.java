package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.EncounterConditionValueExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterConditionValue;
import jakarta.annotation.Resource;

/// 遭遇条件值数据写入器测试
///
/// 测试 EncounterConditionValueDataWriter 的功能，包括数据获取和Excel写入
class EncounterConditionValueDataWriterTest extends AbstractExcelWriterTest {

  @Resource
  private PokeApiDataProvider<EncounterConditionValue> encounterConditionValueDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return ENCOUNTER_CONDITION_VALUE 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.ENCOUNTER_CONDITION_VALUE;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return EncounterConditionValueExcelDTO.class
  @Override
  Class<EncounterConditionValueExcelDTO> getExcelClass() {
    return EncounterConditionValueExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 遭遇条件值数据提供者实例
  @Override
  PokeApiDataProvider<EncounterConditionValue> getDataProvider() {
    return encounterConditionValueDataProvider;
  }
}

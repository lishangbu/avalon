package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.EncounterConditionExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterCondition;
import jakarta.annotation.Resource;

/// 遭遇条件数据写入器测试
///
/// 测试 EncounterConditionDataWriter 的功能，包括数据获取和Excel写入
class EncounterConditionDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<EncounterCondition> encounterConditionDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return ENCOUNTER_CONDITION 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.ENCOUNTER_CONDITION;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return EncounterConditionExcelDTO.class
    @Override
    Class<EncounterConditionExcelDTO> getExcelClass() {
        return EncounterConditionExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 遭遇条件数据提供者实例
    @Override
    PokeApiDataProvider<EncounterCondition> getDataProvider() {
        return encounterConditionDataProvider;
    }
}

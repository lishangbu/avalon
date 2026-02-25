package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.EncounterMethodExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.encounter.EncounterMethod;
import jakarta.annotation.Resource;

/// 遭遇方式数据写入器测试
///
/// 测试 EncounterMethodDataWriter 的功能，包括数据获取和Excel写入
class EncounterMethodDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<EncounterMethod> encounterMethodDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return ENCOUNTER_METHOD 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.ENCOUNTER_METHOD;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return EncounterMethodExcelDTO.class
    @Override
    Class<EncounterMethodExcelDTO> getExcelClass() {
        return EncounterMethodExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 遭遇方式数据提供者实例
    @Override
    PokeApiDataProvider<EncounterMethod> getDataProvider() {
        return encounterMethodDataProvider;
    }
}

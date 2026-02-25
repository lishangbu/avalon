package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.StatExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Stat;
import jakarta.annotation.Resource;

/// 属性数据写入器测试
///
/// 测试 StatDataWriter 的功能，包括数据获取和Excel写入
class StatDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<Stat> statDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return STAT 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.STAT;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return StatExcelDTO.class
    @Override
    Class<StatExcelDTO> getExcelClass() {
        return StatExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 属性数据提供者实例
    @Override
    PokeApiDataProvider<Stat> getDataProvider() {
        return statDataProvider;
    }
}

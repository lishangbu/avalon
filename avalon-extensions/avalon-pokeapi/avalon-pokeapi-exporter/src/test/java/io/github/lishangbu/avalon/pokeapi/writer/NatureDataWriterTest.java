package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.NatureExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Nature;
import jakarta.annotation.Resource;

/// 性格数据写入器测试
///
/// 测试 NatureDataWriter 的功能，包括数据获取和Excel写入
class NatureDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<Nature> natureDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return NATURE 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.NATURE;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return NatureExcelDTO.class
    @Override
    Class<NatureExcelDTO> getExcelClass() {
        return NatureExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 性格数据提供者实例
    @Override
    PokeApiDataProvider<Nature> getDataProvider() {
        return natureDataProvider;
    }
}

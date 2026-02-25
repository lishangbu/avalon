package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.RegionExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.location.Region;
import jakarta.annotation.Resource;

/// 地区数据写入器测试
///
/// 测试 RegionDataWriter 的功能，包括数据获取和Excel写入
class RegionDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<Region> regionDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return REGION 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.REGION;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return RegionExcelDTO.class
    @Override
    Class<RegionExcelDTO> getExcelClass() {
        return RegionExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 地区数据提供者实例
    @Override
    PokeApiDataProvider<Region> getDataProvider() {
        return regionDataProvider;
    }
}

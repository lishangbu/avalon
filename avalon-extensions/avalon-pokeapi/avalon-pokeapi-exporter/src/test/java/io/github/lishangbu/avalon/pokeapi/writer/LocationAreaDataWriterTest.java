package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.LocationAreaExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.location.LocationArea;
import jakarta.annotation.Resource;

/// 位置区域数据写入器测试
///
/// 测试 LocationAreaDataWriter 的功能，包括数据获取和Excel写入
class LocationAreaDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<LocationArea> locationAreaDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return LOCATION_AREA 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.LOCATION_AREA;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return LocationAreaExcelDTO.class
  @Override
  Class<LocationAreaExcelDTO> getExcelClass() {
    return LocationAreaExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 位置区域数据提供者实例
  @Override
  PokeApiDataProvider<LocationArea> getDataProvider() {
    return locationAreaDataProvider;
  }
}

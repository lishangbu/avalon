package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.ItemAttributeExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.item.ItemAttribute;
import jakarta.annotation.Resource;

/// 道具属性数据写入器测试
///
/// 测试 ItemAttributeDataWriter 的功能，包括数据获取和Excel写入
class ItemAttributeDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<ItemAttribute> itemAttributeDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return ITEM_ATTRIBUTE 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.ITEM_ATTRIBUTE;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return ItemAttributeExcelDTO.class
  @Override
  Class<ItemAttributeExcelDTO> getExcelClass() {
    return ItemAttributeExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 道具属性数据提供者实例
  @Override
  PokeApiDataProvider<ItemAttribute> getDataProvider() {
    return itemAttributeDataProvider;
  }
}

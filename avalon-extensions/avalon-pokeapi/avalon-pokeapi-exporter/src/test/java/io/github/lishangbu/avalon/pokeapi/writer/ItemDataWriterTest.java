package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.ItemExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import jakarta.annotation.Resource;

/// 道具数据写入器测试
///
/// 测试 ItemDataWriter 的功能，包括数据获取和Excel写入
class ItemDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<Item> itemDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return ITEM 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.ITEM;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return ItemExcelDTO.class
  @Override
  Class<ItemExcelDTO> getExcelClass() {
    return ItemExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 道具数据提供者实例
  @Override
  PokeApiDataProvider<Item> getDataProvider() {
    return itemDataProvider;
  }
}

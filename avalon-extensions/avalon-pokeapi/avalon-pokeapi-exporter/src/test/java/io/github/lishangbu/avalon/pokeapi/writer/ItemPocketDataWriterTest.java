package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.ItemPocketExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.item.ItemPocket;
import jakarta.annotation.Resource;

/// 道具口袋数据写入器测试
///
/// 测试 ItemPocketDataWriter 的功能，包括数据获取和Excel写入
class ItemPocketDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<ItemPocket> itemPocketDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return ITEM_POCKET 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.ITEM_POCKET;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return ItemPocketExcelDTO.class
  @Override
  Class<ItemPocketExcelDTO> getExcelClass() {
    return ItemPocketExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 道具口袋数据提供者实例
  @Override
  PokeApiDataProvider<ItemPocket> getDataProvider() {
    return itemPocketDataProvider;
  }
}

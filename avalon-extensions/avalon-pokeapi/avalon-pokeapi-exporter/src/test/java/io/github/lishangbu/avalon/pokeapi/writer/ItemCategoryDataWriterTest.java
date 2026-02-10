package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.ItemCategoryExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.item.ItemCategory;
import jakarta.annotation.Resource;

/// 道具类别数据写入器测试
///
/// 测试 ItemCategoryDataWriter 的功能，包括数据获取和Excel写入
class ItemCategoryDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<ItemCategory> itemCategoryDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return ITEM_CATEGORY 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.ITEM_CATEGORY;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return ItemCategoryExcelDTO.class
  @Override
  Class<ItemCategoryExcelDTO> getExcelClass() {
    return ItemCategoryExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 道具类别数据提供者实例
  @Override
  PokeApiDataProvider<ItemCategory> getDataProvider() {
    return itemCategoryDataProvider;
  }
}

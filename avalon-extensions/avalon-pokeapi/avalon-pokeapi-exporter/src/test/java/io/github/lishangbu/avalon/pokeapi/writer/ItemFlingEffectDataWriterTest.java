package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.ItemFlingEffectExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.item.ItemFlingEffect;
import jakarta.annotation.Resource;

/// 道具投掷效果数据写入器测试
///
/// 测试 ItemFlingEffectDataWriter 的功能，包括数据获取和Excel写入
class ItemFlingEffectDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<ItemFlingEffect> itemFlingEffectDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return ITEM_FLING_EFFECT 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.ITEM_FLING_EFFECT;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return ItemFlingEffectExcelDTO.class
    @Override
    Class<ItemFlingEffectExcelDTO> getExcelClass() {
        return ItemFlingEffectExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 道具投掷效果数据提供者实例
    @Override
    PokeApiDataProvider<ItemFlingEffect> getDataProvider() {
        return itemFlingEffectDataProvider;
    }
}

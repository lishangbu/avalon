package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.MoveCategoryExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveCategory;
import jakarta.annotation.Resource;

/// 招式分类数据写入器测试
///
/// 测试 MoveCategoryDataWriter 的功能，包括数据获取和Excel写入
class MoveCategoryDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<MoveCategory> moveCategoryDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return MOVE_CATEGORY 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.MOVE_CATEGORY;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return MoveCategoryExcelDTO.class
    @Override
    Class<MoveCategoryExcelDTO> getExcelClass() {
        return MoveCategoryExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 招式分类数据提供者实例
    @Override
    PokeApiDataProvider<MoveCategory> getDataProvider() {
        return moveCategoryDataProvider;
    }
}

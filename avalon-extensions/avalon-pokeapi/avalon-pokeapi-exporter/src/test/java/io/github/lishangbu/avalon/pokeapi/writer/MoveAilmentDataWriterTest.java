package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.MoveAilmentExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveAilment;
import jakarta.annotation.Resource;

/// 招式异常数据写入器测试
///
/// 测试 MoveAilmentDataWriter 的功能，包括数据获取和Excel写入
class MoveAilmentDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<MoveAilment> moveAilmentDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return MOVE_AILMENT 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.MOVE_AILMENT;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return MoveAilmentExcelDTO.class
    @Override
    Class<MoveAilmentExcelDTO> getExcelClass() {
        return MoveAilmentExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 招式异常数据提供者实例
    @Override
    PokeApiDataProvider<MoveAilment> getDataProvider() {
        return moveAilmentDataProvider;
    }
}

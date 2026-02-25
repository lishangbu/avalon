package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.MoveBattleStyleExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveBattleStyle;
import jakarta.annotation.Resource;

/// 招式战斗风格数据写入器测试
///
/// 测试 MoveBattleStyleDataWriter 的功能，包括数据获取和Excel写入
class MoveBattleStyleDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<MoveBattleStyle> moveBattleStyleDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return MOVE_BATTLE_STYLE 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.MOVE_BATTLE_STYLE;
    }

    /// 返回Excel 数据传输对象类
    ///
    /// @return MoveBattleStyleExcelDTO.class
    @Override
    Class<MoveBattleStyleExcelDTO> getExcelClass() {
        return MoveBattleStyleExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 招式战斗风格数据提供者实例
    @Override
    PokeApiDataProvider<MoveBattleStyle> getDataProvider() {
        return moveBattleStyleDataProvider;
    }
}

package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.MoveDamageClassExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveDamageClass;
import jakarta.annotation.Resource;

/// 招式伤害类别数据写入器测试
///
/// 测试 MoveDamageClassDataWriter 的功能，包括数据获取和Excel写入
class MoveDamageClassDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<MoveDamageClass> moveDamageClassDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return MOVE_DAMAGE_CLASS 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.MOVE_DAMAGE_CLASS;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return MoveDamageClassExcelDTO.class
    @Override
    Class<MoveDamageClassExcelDTO> getExcelClass() {
        return MoveDamageClassExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 招式伤害类别数据提供者实例
    @Override
    PokeApiDataProvider<MoveDamageClass> getDataProvider() {
        return moveDamageClassDataProvider;
    }
}

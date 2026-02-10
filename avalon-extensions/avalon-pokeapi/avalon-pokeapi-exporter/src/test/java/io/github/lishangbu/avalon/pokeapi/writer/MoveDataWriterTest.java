package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.MoveExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.Move;
import jakarta.annotation.Resource;

/// 招式数据写入器测试
///
/// 测试 MoveDataWriter 的功能，包括数据获取和Excel写入
class MoveDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<Move> moveDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return MOVE 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.MOVE;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return MoveExcelDTO.class
  @Override
  Class<MoveExcelDTO> getExcelClass() {
    return MoveExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 招式数据提供者实例
  @Override
  PokeApiDataProvider<Move> getDataProvider() {
    return moveDataProvider;
  }
}

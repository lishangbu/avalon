package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.MoveTargetExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveTarget;
import jakarta.annotation.Resource;

/// 招式目标数据写入器测试
///
/// 测试 MoveTargetDataWriter 的功能，包括数据获取和Excel写入
class MoveTargetDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<MoveTarget> moveTargetDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return MOVE_TARGET 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.MOVE_TARGET;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return MoveTargetExcelDTO.class
  @Override
  Class<MoveTargetExcelDTO> getExcelClass() {
    return MoveTargetExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 招式目标数据提供者实例
  @Override
  PokeApiDataProvider<MoveTarget> getDataProvider() {
    return moveTargetDataProvider;
  }
}

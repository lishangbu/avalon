package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.MoveLearnMethodExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveLearnMethod;
import jakarta.annotation.Resource;

/// 招式学习方法数据写入器测试
///
/// 测试 MoveLearnMethodDataWriter 的功能，包括数据获取和Excel写入
class MoveLearnMethodDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<MoveLearnMethod> moveLearnMethodDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return MOVE_LEARN_METHOD 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.MOVE_LEARN_METHOD;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return MoveLearnMethodExcelDTO.class
  @Override
  Class<MoveLearnMethodExcelDTO> getExcelClass() {
    return MoveLearnMethodExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 招式学习方法数据提供者实例
  @Override
  PokeApiDataProvider<MoveLearnMethod> getDataProvider() {
    return moveLearnMethodDataProvider;
  }
}

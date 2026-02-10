package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.MachineExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.machine.Machine;
import jakarta.annotation.Resource;

/// 机器数据写入器测试
///
/// 测试 MachineDataWriter 的功能，包括数据获取和Excel写入
class MachineDataWriterTest extends AbstractExcelWriterTest {

  @Resource private PokeApiDataProvider<Machine> machineDataProvider;

  /// 返回数据类型枚举
  ///
  /// @return MACHINE 枚举值
  @Override
  PokeDataTypeEnum getDataTypeEnum() {
    return PokeDataTypeEnum.MACHINE;
  }

  /// 返回Excel数据传输对象类
  ///
  /// @return MachineExcelDTO.class
  @Override
  Class<MachineExcelDTO> getExcelClass() {
    return MachineExcelDTO.class;
  }

  /// 返回数据提供者
  ///
  /// @return 机器数据提供者实例
  @Override
  PokeApiDataProvider<Machine> getDataProvider() {
    return machineDataProvider;
  }
}

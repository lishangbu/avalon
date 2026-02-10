package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.MachineExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.machine.Machine;
import org.springframework.stereotype.Service;

/// 机器数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class MachineDataProvider extends AbstractPokeApiDataProvider<Machine, MachineExcelDTO> {

  @Override
  public MachineExcelDTO convert(Machine machine) {
    MachineExcelDTO result = new MachineExcelDTO();
    result.setId(machine.id());
    result.setItemName(machine.item().name());
    result.setMoveName(machine.move().name());
    result.setVersionGroupName(machine.versionGroup().name());
    return result;
  }
}

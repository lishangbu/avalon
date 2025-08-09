package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.MACHINE;
import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.MOVE;

import io.github.lishangbu.avalon.dataset.entity.Machine;
import io.github.lishangbu.avalon.dataset.repository.*;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Transactional;

/**
 * 技能学习机器数据处理命令
 *
 * @author lishangbu
 * @since 2025/8/8
 */
@ShellComponent
public class MachineDataSetShellComponent extends AbstractDataSetShellComponent {
  /**
   * 单个中文汉字<br>
   * 参照维基百科汉字Unicode范围(https://zh.wikipedia.org/wiki/%E6%B1%89%E5%AD%97 页面右侧)
   */
  private static final String NUMBER_REGEX = "\\d+";

  private final PokeApiFactory pokeApiFactory;
  private final ItemRepository itemRepository;
  private final MachineRepository machineRepository;
  private final MoveRepository moveRepository;

  public MachineDataSetShellComponent(
      PokeApiFactory pokeApiFactory,
      ItemRepository itemRepository,
      MachineRepository machineRepository,
      MoveRepository moveRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.itemRepository = itemRepository;
    this.machineRepository = machineRepository;
    this.moveRepository = moveRepository;
  }

  @ShellMethod(key = "dataset refresh machine", value = "刷新数据库中的技能机器表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData() {
    NamedAPIResourceList namedApiResources = pokeApiFactory.getPagedResource(MOVE);
    return super.saveEntityData(
        namedApiResources.results(),
        this::convertToMachine,
        machineRepository,
        machine -> machine.getItem().getName());
  }

  private Machine convertToMachine(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.machine.Machine apiResult =
        pokeApiFactory.getSingleResource(MACHINE, NamedApiResourceUtils.getId(namedApiResource));
    Machine machine = new Machine();
    machine.setId(apiResult.id());
    itemRepository.findByInternalName(apiResult.item().name()).ifPresent(machine::setItem);
    moveRepository.findByInternalName(apiResult.move().name()).ifPresent(machine::setMove);

    return machine;
  }
}

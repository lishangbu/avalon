package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.MOVE_TARGET;

import io.github.lishangbu.avalon.dataset.entity.MoveTarget;
import io.github.lishangbu.avalon.dataset.repository.MoveTargetRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.transaction.annotation.Transactional;

/**
 * 战斗招式目标数据处理命令
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@ShellComponent
public class MoveTargetDataSetShellComponent extends AbstractDataSetShellComponent {
  private final PokeApiFactory pokeApiFactory;
  private final MoveTargetRepository moveTargetRepository;

  public MoveTargetDataSetShellComponent(
      PokeApiFactory pokeApiFactory, MoveTargetRepository moveTargetRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.moveTargetRepository = moveTargetRepository;
  }

  @ShellMethod(key = "dataset refresh moveTarget", value = "刷新数据库中的战斗招式目标表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData(
      @ShellOption(help = "每页偏移量", defaultValue = "0") Integer offset,
      @ShellOption(help = "每页数量", defaultValue = "100") Integer limit) {
    NamedAPIResourceList namedApiResources =
        pokeApiFactory.getPagedResource(MOVE_TARGET, offset, limit);
    return super.saveEntityData(
        namedApiResources.results(),
        this::convertToMoveTarget,
        moveTargetRepository,
        MoveTarget::getName);
  }

  private MoveTarget convertToMoveTarget(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.move.MoveTarget apiResult =
        pokeApiFactory.getSingleResource(MOVE_TARGET, namedApiResource.name());
    MoveTarget moveTarget = new MoveTarget();
    moveTarget.setId(apiResult.id());
    moveTarget.setInternalName(apiResult.name());
    LocalizationUtils.getLocalizationName(apiResult.names())
        .ifPresentOrElse(
            name -> {
              moveTarget.setName(name.name());
            },
            () -> moveTarget.setName(apiResult.name()));
    LocalizationUtils.getLocalizationDescription(apiResult.descriptions())
        .ifPresentOrElse(
            description -> moveTarget.setDescription(description.description()),
            () -> moveTarget.setDescription(""));
    return moveTarget;
  }
}

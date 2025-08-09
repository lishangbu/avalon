package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.MOVE_AILMENT;

import io.github.lishangbu.avalon.dataset.entity.MoveAilment;
import io.github.lishangbu.avalon.dataset.repository.MoveAilmentRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Transactional;

/**
 * 招式状态异常数据处理命令
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@ShellComponent
public class MoveAilmentDataSetShellComponent extends AbstractDataSetShellComponent {
  private final PokeApiFactory pokeApiFactory;
  private final MoveAilmentRepository moveAilmentRepository;

  public MoveAilmentDataSetShellComponent(
      PokeApiFactory pokeApiFactory, MoveAilmentRepository moveAilmentRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.moveAilmentRepository = moveAilmentRepository;
  }

  @ShellMethod(key = "dataset refresh moveAilment", value = "刷新数据库中的招式状态异常表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData() {
    NamedAPIResourceList namedApiResources = pokeApiFactory.getPagedResource(MOVE_AILMENT);
    return super.saveEntityData(
        namedApiResources.results(),
        this::convertToMoveAilment,
        moveAilmentRepository,
        MoveAilment::getName);
  }

  private MoveAilment convertToMoveAilment(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.move.MoveAilment apiResult =
        pokeApiFactory.getSingleResource(
            MOVE_AILMENT, NamedApiResourceUtils.getId(namedApiResource));
    MoveAilment moveAilment = new MoveAilment();
    moveAilment.setId(apiResult.id());
    moveAilment.setInternalName(apiResult.name());
    LocalizationUtils.getLocalizationName(apiResult.names())
        .ifPresent(
            name -> {
              moveAilment.setName(name.name());
            });
    return moveAilment;
  }
}

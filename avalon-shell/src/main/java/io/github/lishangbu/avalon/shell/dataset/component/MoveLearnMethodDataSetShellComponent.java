package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.MOVE_LEARN_METHOD;

import io.github.lishangbu.avalon.dataset.entity.MoveLearnMethod;
import io.github.lishangbu.avalon.dataset.repository.MoveLearnMethodRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.transaction.annotation.Transactional;

/**
 * 招式学习方法数据处理命令
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@ShellComponent
public class MoveLearnMethodDataSetShellComponent extends AbstractDataSetShellComponent {
  private final PokeApiFactory pokeApiFactory;
  private final MoveLearnMethodRepository moveLearnMethodRepository;

  public MoveLearnMethodDataSetShellComponent(
      PokeApiFactory pokeApiFactory, MoveLearnMethodRepository moveLearnMethodRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.moveLearnMethodRepository = moveLearnMethodRepository;
  }

  @ShellMethod(key = "dataset refresh moveLearnMethod", value = "刷新数据库中的招式学习方法表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData(
      @ShellOption(help = "每页偏移量", defaultValue = "0") Integer offset,
      @ShellOption(help = "每页数量", defaultValue = "100") Integer limit) {
    NamedAPIResourceList namedApiResources =
        pokeApiFactory.getPagedResource(MOVE_LEARN_METHOD, offset, limit);
    return super.saveEntityData(
        namedApiResources.results(),
        this::convertToMoveLearnMethod,
        moveLearnMethodRepository,
        MoveLearnMethod::getName);
  }

  private MoveLearnMethod convertToMoveLearnMethod(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.move.MoveLearnMethod apiResult =
        pokeApiFactory.getSingleResource(MOVE_LEARN_METHOD, namedApiResource.name());
    MoveLearnMethod moveLearnMethod = new MoveLearnMethod();
    moveLearnMethod.setId(apiResult.id());
    moveLearnMethod.setInternalName(apiResult.name());
    LocalizationUtils.getLocalizationName(apiResult.names())
        .ifPresent(
            name -> {
              moveLearnMethod.setName(name.name());
            });
    LocalizationUtils.getLocalizationDescription(apiResult.descriptions())
        .ifPresent(description -> moveLearnMethod.setDescription(description.description()));
    return moveLearnMethod;
  }
}

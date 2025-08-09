package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.MOVE_DAMAGE_CLASS;

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass;
import io.github.lishangbu.avalon.dataset.repository.MoveDamageClassRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Transactional;

/**
 * 招式伤害类别数据处理命令
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@ShellComponent
public class MoveDamageClassDataSetShellComponent extends AbstractDataSetShellComponent {
  private final PokeApiFactory pokeApiFactory;
  private final MoveDamageClassRepository moveDamageClassRepository;

  public MoveDamageClassDataSetShellComponent(
      PokeApiFactory pokeApiFactory, MoveDamageClassRepository moveTargetRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.moveDamageClassRepository = moveTargetRepository;
  }

  @ShellMethod(key = "dataset refresh moveDamageClass", value = "刷新数据库中的招式伤害类别表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData() {
    NamedAPIResourceList namedApiResources = pokeApiFactory.getPagedResource(MOVE_DAMAGE_CLASS);
    return super.saveEntityData(
        namedApiResources.results(),
        this::convertToMoveDamageClass,
        moveDamageClassRepository,
        MoveDamageClass::getName);
  }

  private MoveDamageClass convertToMoveDamageClass(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.move.MoveDamageClass apiResult =
        pokeApiFactory.getSingleResource(
            MOVE_DAMAGE_CLASS, NamedApiResourceUtils.getId(namedApiResource));
    MoveDamageClass moveDamageClass = new MoveDamageClass();
    moveDamageClass.setId(apiResult.id());
    moveDamageClass.setInternalName(apiResult.name());
    LocalizationUtils.getLocalizationName(apiResult.names())
        .ifPresent(
            name -> {
              moveDamageClass.setName(name.name());
            });
    LocalizationUtils.getLocalizationDescription(apiResult.descriptions())
        .ifPresent(description -> moveDamageClass.setDescription(description.description()));
    return moveDamageClass;
  }
}

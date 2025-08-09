package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.MOVE_CATEGORY;

import io.github.lishangbu.avalon.dataset.entity.MoveCategory;
import io.github.lishangbu.avalon.dataset.repository.MoveCategoryRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Transactional;

/**
 * 招式松散分类数据处理命令
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@ShellComponent
public class MoveCategoryDataSetShellComponent extends AbstractDataSetShellComponent {
  private final PokeApiFactory pokeApiFactory;
  private final MoveCategoryRepository moveCategoryRepository;

  public MoveCategoryDataSetShellComponent(
      PokeApiFactory pokeApiFactory, MoveCategoryRepository moveCategoryRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.moveCategoryRepository = moveCategoryRepository;
  }

  @ShellMethod(key = "dataset refresh moveCategory", value = "刷新数据库中的招式松散分类表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData() {
    NamedAPIResourceList namedApiResources = pokeApiFactory.getPagedResource(MOVE_CATEGORY);
    return super.saveEntityData(
        namedApiResources.results(),
        this::convertToMoveCategory,
        moveCategoryRepository,
        MoveCategory::getName);
  }

  private MoveCategory convertToMoveCategory(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.move.MoveCategory apiResult =
        pokeApiFactory.getSingleResource(
            MOVE_CATEGORY, NamedApiResourceUtils.getId(namedApiResource));
    MoveCategory moveCategory = new MoveCategory();
    moveCategory.setId(apiResult.id());
    moveCategory.setInternalName(apiResult.name());
    moveCategory.setName(apiResult.name());
    LocalizationUtils.getLocalizationDescription(apiResult.descriptions())
        .ifPresent(description -> moveCategory.setDescription(description.description()));
    return moveCategory;
  }
}

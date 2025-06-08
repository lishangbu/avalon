package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.ITEM_CATEGORY;

import io.github.lishangbu.avalon.dataset.entity.ItemCategory;
import io.github.lishangbu.avalon.dataset.repository.ItemCategoryRepository;
import io.github.lishangbu.avalon.dataset.repository.ItemPocketRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.transaction.annotation.Transactional;

/**
 * 道具分类数据处理命令
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@ShellComponent
public class ItemCategoryDataSetShellComponent extends AbstractDataSetShellComponent {
  private final PokeApiFactory pokeApiFactory;
  private final ItemCategoryRepository itemCategoryRepository;
  private final ItemPocketRepository itemPocketRepository;

  public ItemCategoryDataSetShellComponent(
      PokeApiFactory pokeApiFactory,
      ItemCategoryRepository itemCategoryRepository,
      ItemPocketRepository itemPocketRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.itemCategoryRepository = itemCategoryRepository;
    this.itemPocketRepository = itemPocketRepository;
  }

  @ShellMethod(key = "dataset refresh itemCategory", value = "刷新数据库中的道具分类表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData(
      @ShellOption(help = "每页偏移量", defaultValue = "0") Integer offset,
      @ShellOption(help = "每页数量", defaultValue = "100") Integer limit) {
    NamedAPIResourceList namedApiResources =
        pokeApiFactory.getPagedResource(ITEM_CATEGORY, offset, limit);
    return super.saveEntityData(
        namedApiResources.results(),
        this::convertToItemCategory,
        itemCategoryRepository,
        ItemCategory::getName);
  }

  private ItemCategory convertToItemCategory(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.item.ItemCategory apiResult =
        pokeApiFactory.getSingleResource(ITEM_CATEGORY, namedApiResource.name());
    ItemCategory itemCategory = new ItemCategory();
    itemCategory.setId(apiResult.id());
    itemCategory.setInternalName(apiResult.name());
    itemPocketRepository
        .findByInternalName(apiResult.pocket().name())
        .ifPresent(itemPocket -> itemCategory.setItemPocket(itemPocket));
    LocalizationUtils.getLocalizationName(
            apiResult.names(), LocalizationUtils.SIMPLIFIED_CHINESE, LocalizationUtils.ENGLISH)
        .ifPresentOrElse(
            name -> {
              itemCategory.setName(name.name());
            },
            () -> {
              itemCategory.setName(apiResult.name());
            });
    return itemCategory;
  }
}

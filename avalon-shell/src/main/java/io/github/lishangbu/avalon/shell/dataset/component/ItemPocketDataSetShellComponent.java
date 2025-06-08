package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.ITEM_POCKET;

import io.github.lishangbu.avalon.dataset.entity.ItemPocket;
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
 * 道具口袋数据处理命令
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@ShellComponent
public class ItemPocketDataSetShellComponent extends AbstractDataSetShellComponent {
  private final PokeApiFactory pokeApiFactory;
  private final ItemPocketRepository itemPocketRepository;

  public ItemPocketDataSetShellComponent(
      PokeApiFactory pokeApiFactory, ItemPocketRepository itemPocketRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.itemPocketRepository = itemPocketRepository;
  }

  @ShellMethod(key = "dataset refresh itemPocket", value = "刷新数据库中的道具口袋表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData(
      @ShellOption(help = "每页偏移量", defaultValue = "0") Integer offset,
      @ShellOption(help = "每页数量", defaultValue = "100") Integer limit) {
    NamedAPIResourceList namedApiResources =
        pokeApiFactory.getPagedResource(ITEM_POCKET, offset, limit);
    return super.saveEntityData(
        namedApiResources.results(),
        this::convertToItemPocket,
        itemPocketRepository,
        ItemPocket::getName);
  }

  private ItemPocket convertToItemPocket(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.item.ItemPocket apiResult =
        pokeApiFactory.getSingleResource(ITEM_POCKET, namedApiResource.name());
    ItemPocket itemPocket = new ItemPocket();
    itemPocket.setId(apiResult.id());
    itemPocket.setInternalName(apiResult.name());
    LocalizationUtils.getLocalizationName(apiResult.names())
        .ifPresentOrElse(
            name -> {
              itemPocket.setName(name.name());
            },
            () -> {
              itemPocket.setName(apiResult.name());
            });
    return itemPocket;
  }
}

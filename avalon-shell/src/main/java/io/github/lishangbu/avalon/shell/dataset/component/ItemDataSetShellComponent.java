package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.ITEM;

import io.github.lishangbu.avalon.dataset.entity.Item;
import io.github.lishangbu.avalon.dataset.entity.ItemAttribute;
import io.github.lishangbu.avalon.dataset.repository.ItemAttributeRepository;
import io.github.lishangbu.avalon.dataset.repository.ItemCategoryRepository;
import io.github.lishangbu.avalon.dataset.repository.ItemFlingEffectRepository;
import io.github.lishangbu.avalon.dataset.repository.ItemRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Transactional;

/**
 * 道具数据处理命令
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@ShellComponent
public class ItemDataSetShellComponent extends AbstractDataSetShellComponent {
  private final PokeApiFactory pokeApiFactory;
  private final ItemRepository itemRepository;
  private final ItemFlingEffectRepository itemFlingEffectRepository;
  private final ItemAttributeRepository itemAttributeRepository;

  private final ItemCategoryRepository itemCategoryRepository;

  public ItemDataSetShellComponent(
      PokeApiFactory pokeApiFactory,
      ItemRepository itemRepository,
      ItemFlingEffectRepository itemFlingEffectRepository,
      ItemAttributeRepository itemAttributeRepository,
      ItemCategoryRepository itemCategoryRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.itemRepository = itemRepository;
    this.itemFlingEffectRepository = itemFlingEffectRepository;
    this.itemAttributeRepository = itemAttributeRepository;
    this.itemCategoryRepository = itemCategoryRepository;
  }

  @ShellMethod(key = "dataset refresh item", value = "刷新数据库中的道具表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData() {
    NamedAPIResourceList namedApiResources = pokeApiFactory.getPagedResource(ITEM);
    return super.saveEntityData(
        namedApiResources.results(), this::convertToItem, itemRepository, Item::getName);
  }

  private Item convertToItem(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.item.Item apiResult =
        pokeApiFactory.getSingleResource(ITEM, NamedApiResourceUtils.getId(namedApiResource));
    Item item = new Item();
    item.setId(apiResult.id());
    item.setInternalName(apiResult.name());
    LocalizationUtils.getLocalizationName(apiResult.names())
        .ifPresentOrElse(
            localizationName -> item.setName(localizationName.name()),
            () -> item.setName(apiResult.name()));
    item.setCost(apiResult.cost());
    item.setFlingPower(apiResult.flingPower());
    if (apiResult.flingEffect() != null) {
      itemFlingEffectRepository
          .findByInternalName(apiResult.flingEffect().name())
          .ifPresent(itemFlingEffect -> item.setFlingEffect(itemFlingEffect));
    }
    if (apiResult.attributes() != null && !apiResult.attributes().isEmpty()) {
      List<ItemAttribute> attributes = new ArrayList<>();
      for (NamedApiResource attribute : apiResult.attributes()) {
        itemAttributeRepository
            .findByInternalName(attribute.name())
            .ifPresent(itemAttribute -> attributes.add(itemAttribute));
      }
      if (!attributes.isEmpty()) {
        item.setAttributes(attributes);
      }
    }
    itemCategoryRepository
        .findByInternalName(apiResult.category().name())
        .ifPresent(itemCategory -> item.setCategory(itemCategory));
    LocalizationUtils.getLocalizationVerboseEffect(apiResult.effectEntries())
        .ifPresent(
            verboseEffect -> {
              item.setShortEffect(verboseEffect.shortEffect());
              item.setEffect(verboseEffect.effect());
            });
    LocalizationUtils.getLocalizationVersionGroupFlavorText(
            apiResult.flavorTextEntries().reversed())
        .ifPresent(versionGroupFlavorText -> item.setText(versionGroupFlavorText.text()));
    return item;
  }
}

package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.ITEM_ATTRIBUTE;

import io.github.lishangbu.avalon.dataset.entity.ItemAttribute;
import io.github.lishangbu.avalon.dataset.repository.ItemAttributeRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Transactional;

/**
 * 道具口袋数据处理命令
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@ShellComponent
public class ItemAttributeDataSetShellComponent extends AbstractDataSetShellComponent {
  private final PokeApiFactory pokeApiFactory;
  private final ItemAttributeRepository itemAttributeRepository;

  public ItemAttributeDataSetShellComponent(
      PokeApiFactory pokeApiFactory, ItemAttributeRepository itemAttributeRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.itemAttributeRepository = itemAttributeRepository;
  }

  @ShellMethod(key = "dataset refresh itemAttribute", value = "刷新数据库中的道具属性表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData() {
    NamedAPIResourceList namedApiResources = pokeApiFactory.getPagedResource(ITEM_ATTRIBUTE);
    return super.saveEntityData(
        namedApiResources.results(),
        this::convertToItemAttribute,
        itemAttributeRepository,
        ItemAttribute::getName);
  }

  private ItemAttribute convertToItemAttribute(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.item.ItemAttribute apiResult =
        pokeApiFactory.getSingleResource(
            ITEM_ATTRIBUTE, NamedApiResourceUtils.getId(namedApiResource));
    ItemAttribute itemAttribute = new ItemAttribute();
    itemAttribute.setId(apiResult.id());
    itemAttribute.setInternalName(apiResult.name());
    LocalizationUtils.getLocalizationName(apiResult.names())
        .ifPresent(
            name -> {
              itemAttribute.setName(name.name());
            });
    LocalizationUtils.getLocalizationDescription(apiResult.descriptions())
        .ifPresent(
            description -> {
              itemAttribute.setDescription(description.description());
            });
    return itemAttribute;
  }
}

package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.Item;
import io.github.lishangbu.avalon.dataset.entity.ItemAttribute;
import io.github.lishangbu.avalon.dataset.repository.ItemAttributeRepository;
import io.github.lishangbu.avalon.dataset.repository.ItemCategoryRepository;
import io.github.lishangbu.avalon.dataset.repository.ItemFlingEffectRepository;
import io.github.lishangbu.avalon.dataset.repository.ItemRepository;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * 道具数据集处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class ItemDataSetParseStrategy implements BasicDataSetParseStrategy {
  private final ItemRepository itemRepository;

  private final ItemAttributeRepository itemAttributeRepository;

  private final ItemCategoryRepository itemCategoryRepository;

  private final ItemFlingEffectRepository itemFlingEffectRepository;

  public ItemDataSetParseStrategy(
      ItemRepository itemRepository,
      ItemAttributeRepository itemAttributeRepository,
      ItemCategoryRepository itemCategoryRepository,
      ItemFlingEffectRepository itemFlingEffectRepository) {
    this.itemRepository = itemRepository;
    this.itemAttributeRepository = itemAttributeRepository;
    this.itemCategoryRepository = itemCategoryRepository;
    this.itemFlingEffectRepository = itemFlingEffectRepository;
  }

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource instanceof io.github.lishangbu.avalon.pokeapi.model.item.Item itemData) {
      Item item = new Item();
      item.setId(itemData.id());
      item.setInternalName(itemData.name());
      LocalizationUtils.getLocalizationName(itemData.names())
          .ifPresentOrElse(
              localizationName -> item.setName(localizationName.name()),
              () -> item.setName(itemData.name()));
      item.setCost(itemData.cost());
      item.setFlingPower(itemData.flingPower());
      if (itemData.flingEffect() != null) {
        itemFlingEffectRepository
            .findByInternalName(itemData.flingEffect().name())
            .ifPresent(item::setFlingEffect);
      }
      if (itemData.attributes() != null && !itemData.attributes().isEmpty()) {
        List<ItemAttribute> attributes = new ArrayList<>();
        for (NamedApiResource attribute : itemData.attributes()) {
          itemAttributeRepository
              .findByInternalName(attribute.name())
              .ifPresent(itemAttribute -> attributes.add(itemAttribute));
        }
        if (!attributes.isEmpty()) {
          item.setAttributes(attributes);
        }
      }
      itemCategoryRepository
          .findByInternalName(itemData.category().name())
          .ifPresent(itemCategory -> item.setCategory(itemCategory));
      LocalizationUtils.getLocalizationVerboseEffect(itemData.effectEntries())
          .ifPresent(
              verboseEffect -> {
                item.setShortEffect(verboseEffect.shortEffect());
                item.setEffect(verboseEffect.effect());
              });
      LocalizationUtils.getLocalizationVersionGroupFlavorText(
              itemData.flavorTextEntries().reversed())
          .ifPresent(versionGroupFlavorText -> item.setText(versionGroupFlavorText.text()));
      return item;
    }
    return null;
  }

  @Override
  public JpaRepository getRepository() {
    return this.itemRepository;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.ITEM;
  }
}

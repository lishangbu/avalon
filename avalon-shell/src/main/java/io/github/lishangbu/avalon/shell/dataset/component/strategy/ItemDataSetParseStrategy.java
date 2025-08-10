package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.Item;
import io.github.lishangbu.avalon.dataset.entity.ItemAttribute;
import io.github.lishangbu.avalon.dataset.repository.ItemAttributeRepository;
import io.github.lishangbu.avalon.dataset.repository.ItemCategoryRepository;
import io.github.lishangbu.avalon.dataset.repository.ItemFlingEffectRepository;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 道具数据集处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class ItemDataSetParseStrategy implements BasicDataSetParseStrategy {

  private final ItemAttributeRepository itemAttributeRepository;

  private final ItemCategoryRepository itemCategoryRepository;

  private final ItemFlingEffectRepository itemFlingEffectRepository;

  public ItemDataSetParseStrategy(
      ItemAttributeRepository itemAttributeRepository,
      ItemCategoryRepository itemCategoryRepository,
      ItemFlingEffectRepository itemFlingEffectRepository) {
    this.itemAttributeRepository = itemAttributeRepository;
    this.itemCategoryRepository = itemCategoryRepository;
    this.itemFlingEffectRepository = itemFlingEffectRepository;
  }

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource instanceof io.github.lishangbu.avalon.pokeapi.model.item.Item itemData) {
      Item item = new Item();
      item.setId(itemData.id().longValue());
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
            .ifPresent(
                itemFlingEffect ->
                    item.setFlingEffectInternalName(itemFlingEffect.getInternalName()));
      }
      if (itemData.attributes() != null && !itemData.attributes().isEmpty()) {
        List<ItemAttribute> attributes = new ArrayList<>();
        for (NamedApiResource attribute : itemData.attributes()) {
          itemAttributeRepository
              .findByInternalName(attribute.name())
              .ifPresent(itemAttribute -> attributes.add(itemAttribute));
        }
      }
      itemCategoryRepository
          .findByInternalName(itemData.category().name())
          .ifPresent(itemCategory -> item.setCategoryInternalName(itemCategory.getInternalName()));
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
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.ITEM;
  }
}

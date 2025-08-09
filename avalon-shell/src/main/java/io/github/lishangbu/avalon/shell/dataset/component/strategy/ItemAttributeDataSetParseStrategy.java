package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.ItemAttribute;
import io.github.lishangbu.avalon.dataset.repository.ItemAttributeRepository;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * 道具数据集处理策略
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@Service
public class ItemAttributeDataSetParseStrategy implements BasicDataSetParseStrategy {
  private final ItemAttributeRepository itemAttributeRepository;

  public ItemAttributeDataSetParseStrategy(ItemAttributeRepository itemAttributeRepository) {
    this.itemAttributeRepository = itemAttributeRepository;
  }

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource
        instanceof io.github.lishangbu.avalon.pokeapi.model.item.ItemAttribute itemAttributeData) {
      ItemAttribute itemAttribute = new ItemAttribute();
      itemAttribute.setId(itemAttributeData.id());
      itemAttribute.setInternalName(itemAttributeData.name());
      LocalizationUtils.getLocalizationName(itemAttributeData.names())
          .ifPresent(
              name -> {
                itemAttribute.setName(name.name());
              });
      LocalizationUtils.getLocalizationDescription(itemAttributeData.descriptions())
          .ifPresent(
              description -> {
                itemAttribute.setDescription(description.description());
              });
      return itemAttribute;
    }
    return null;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.ITEM_ATTRIBUTE;
  }

  @Override
  public JpaRepository getRepository() {
    return this.itemAttributeRepository;
  }
}

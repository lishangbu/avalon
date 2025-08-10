package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.ItemAttribute;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.stereotype.Service;

/**
 * 道具属性数据集处理策略
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@Service
public class ItemAttributeDataSetParseStrategy implements BasicDataSetParseStrategy {

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource
        instanceof io.github.lishangbu.avalon.pokeapi.model.item.ItemAttribute itemAttributeData) {
      ItemAttribute itemAttribute = new ItemAttribute();
      itemAttribute.setId(itemAttributeData.id().longValue());
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
}

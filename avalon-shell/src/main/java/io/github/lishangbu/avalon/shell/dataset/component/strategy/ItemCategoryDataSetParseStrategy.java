package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.ItemCategory;
import io.github.lishangbu.avalon.dataset.repository.ItemCategoryRepository;
import io.github.lishangbu.avalon.dataset.repository.ItemPocketRepository;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * 道具分类数据集处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class ItemCategoryDataSetParseStrategy implements BasicDataSetParseStrategy {
  private final ItemCategoryRepository itemCategoryRepository;
  private final ItemPocketRepository itemPocketRepository;

  public ItemCategoryDataSetParseStrategy(
      ItemCategoryRepository itemCategoryRepository, ItemPocketRepository itemPocketRepository) {
    this.itemCategoryRepository = itemCategoryRepository;
    this.itemPocketRepository = itemPocketRepository;
  }

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource
        instanceof io.github.lishangbu.avalon.pokeapi.model.item.ItemCategory itemCategoryData) {
      ItemCategory itemCategory = new ItemCategory();
      itemCategory.setId(itemCategoryData.id());
      itemCategory.setInternalName(itemCategoryData.name());
      itemPocketRepository
          .findByInternalName(itemCategoryData.pocket().name())
          .ifPresent(itemPocket -> itemCategory.setItemPocket(itemPocket));
      LocalizationUtils.getLocalizationName(itemCategoryData.names())
          .ifPresentOrElse(
              name -> {
                itemCategory.setName(name.name());
              },
              () -> {
                itemCategory.setName(itemCategoryData.name());
              });
      return itemCategory;
    }
    return null;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.ITEM_CATEGORY;
  }

  @Override
  public JpaRepository getRepository() {
    return this.itemCategoryRepository;
  }
}

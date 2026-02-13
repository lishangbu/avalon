package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.ItemCategoryExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.item.ItemCategory;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.stereotype.Service;

/// 道具类别数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class ItemCategoryDataProvider
    extends AbstractPokeApiDataProvider<ItemCategory, ItemCategoryExcelDTO> {

  @Override
  public ItemCategoryExcelDTO convert(ItemCategory itemCategory) {
    ItemCategoryExcelDTO result = new ItemCategoryExcelDTO();
    result.setId(itemCategory.id());
    result.setInternalName(itemCategory.name());
    result.setName(resolveLocalizedName(itemCategory.names(), itemCategory.name()));
    result.setItemPocketId(NamedApiResourceUtils.getId(itemCategory.pocket()));
    return result;
  }
}

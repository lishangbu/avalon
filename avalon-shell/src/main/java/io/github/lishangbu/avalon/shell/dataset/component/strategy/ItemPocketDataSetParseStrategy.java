package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.ItemPocket;
import io.github.lishangbu.avalon.dataset.repository.ItemPocketRepository;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * 道具口袋数据处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class ItemPocketDataSetParseStrategy implements BasicDataSetParseStrategy {
  private final ItemPocketRepository itemPocketRepository;

  public ItemPocketDataSetParseStrategy(ItemPocketRepository itemPocketRepository) {
    this.itemPocketRepository = itemPocketRepository;
  }

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource
        instanceof io.github.lishangbu.avalon.pokeapi.model.item.ItemPocket itemPocketData) {
      ItemPocket itemPocket = new ItemPocket();
      itemPocket.setId(itemPocketData.id());
      itemPocket.setInternalName(itemPocketData.name());
      LocalizationUtils.getLocalizationName(itemPocketData.names())
          .ifPresentOrElse(
              name -> {
                itemPocket.setName(name.name());
              },
              () -> {
                itemPocket.setName(itemPocketData.name());
              });
      return itemPocket;
    }
    return null;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.ITEM_POCKET;
  }

  @Override
  public JpaRepository getRepository() {
    return this.itemPocketRepository;
  }
}

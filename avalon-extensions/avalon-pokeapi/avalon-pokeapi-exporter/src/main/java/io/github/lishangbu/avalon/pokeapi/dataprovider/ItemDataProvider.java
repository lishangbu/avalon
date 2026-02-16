package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.ItemExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.stereotype.Service;

/// 道具数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class ItemDataProvider extends AbstractPokeApiDataProvider<Item, ItemExcelDTO> {

  @Override
  public ItemExcelDTO convert(Item item) {
    ItemExcelDTO result = new ItemExcelDTO();
    result.setId(item.id());
    result.setInternalName(item.name());
    result.setName(resolveLocalizedName(item.names(), item.name()));
    result.setCost(item.cost());
    result.setFlingPower(item.flingPower());
    result.setCategoryId(
        item.category() != null ? NamedApiResourceUtils.getId(item.category()) : null);
    LocalizationUtils.getLocalizationVerboseEffect(item.effectEntries())
        .ifPresent(
            verboseEffect -> {
              result.setShortEffect(verboseEffect.shortEffect());
              result.setEffect(verboseEffect.effect());
            });
    result.setItemFlingEffect(
        item.flingEffect() != null ? NamedApiResourceUtils.getId(item.flingEffect()) : null);
    result.setFlingPower(item.flingPower());
    LocalizationUtils.getLocalizationVersionGroupFlavorText(item.flavorTextEntries())
        .ifPresent(
            versionGroupFlavorText -> {
              result.setText((versionGroupFlavorText.text()));
            });
    return result;
  }
}

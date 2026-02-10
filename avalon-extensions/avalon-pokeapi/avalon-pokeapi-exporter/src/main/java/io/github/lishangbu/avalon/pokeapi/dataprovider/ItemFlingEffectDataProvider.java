package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.ItemFlingEffectExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.item.ItemFlingEffect;
import org.springframework.stereotype.Service;

/// 道具投掷效果数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class ItemFlingEffectDataProvider
    extends AbstractPokeApiDataProvider<ItemFlingEffect, ItemFlingEffectExcelDTO> {

  @Override
  public ItemFlingEffectExcelDTO convert(ItemFlingEffect itemFlingEffect) {
    ItemFlingEffectExcelDTO result = new ItemFlingEffectExcelDTO();
    result.setId(itemFlingEffect.id());
    result.setInternalName(itemFlingEffect.name());
    result.setEffect(resolveLocalizedEffect(itemFlingEffect.effectEntries()));
    return result;
  }
}

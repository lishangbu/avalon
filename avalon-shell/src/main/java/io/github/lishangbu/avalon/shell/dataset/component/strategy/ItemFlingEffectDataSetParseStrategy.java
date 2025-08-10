package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.ItemFlingEffect;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.stereotype.Service;

/**
 * 道具分类数据集处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class ItemFlingEffectDataSetParseStrategy implements BasicDataSetParseStrategy {

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource
        instanceof
        io.github.lishangbu.avalon.pokeapi.model.item.ItemFlingEffect itemFlingEffectData) {
      ItemFlingEffect itemFlingEffect = new ItemFlingEffect();
      itemFlingEffect.setId(itemFlingEffectData.id());
      itemFlingEffect.setInternalName(itemFlingEffectData.name());
      itemFlingEffect.setName(itemFlingEffectData.name());
      LocalizationUtils.getLocalizationEffect(itemFlingEffectData.effectEntries())
          .ifPresent(
              effect -> {
                itemFlingEffect.setEffect(effect.effect());
              });
      return itemFlingEffect;
    }
    return null;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.ITEM_FLING_EFFECT;
  }
}

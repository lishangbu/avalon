package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.stereotype.Service;

/**
 * 树果硬度数据集处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class BerryFirmnessDataSetParseStrategy implements BasicDataSetParseStrategy {

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource
        instanceof io.github.lishangbu.avalon.pokeapi.model.berry.BerryFirmness berryFirmnessData) {
      BerryFirmness berryFirmness = new BerryFirmness();
      berryFirmness.setId(berryFirmnessData.id());
      berryFirmness.setInternalName(berryFirmnessData.name());
      LocalizationUtils.getLocalizationName(berryFirmnessData.names())
          .ifPresentOrElse(
              name -> {
                berryFirmness.setName(name.name());
              },
              () -> {
                berryFirmness.setName(berryFirmnessData.name());
              });
      return berryFirmness;
    }
    return null;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.BERRY_FIRMNESS;
  }
}

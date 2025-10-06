package io.github.lishangbu.avalon.admin.service.dataset.impl;

import io.github.lishangbu.avalon.admin.service.dataset.BerryFirmnessService;
import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 树果坚硬度服务实现类
 *
 * @author lishangbu
 * @since 2025/10/5
 */
@Service
@RequiredArgsConstructor
public class BerryFirmnessServiceImpl implements BerryFirmnessService {
  private final PokeApiService pokeApiService;
  private final BerryFirmnessRepository berryFirmnessRepository;

  @Override
  public List<BerryFirmness> importBerryFirmnesses() {
    return pokeApiService.importData(
        PokeDataTypeEnum.BERRY_FIRMNESS,
        berryFirmnessData -> {
          BerryFirmness berryFirmness = new BerryFirmness();
          berryFirmness.setInternalName(berryFirmnessData.name());
          berryFirmness.setId(berryFirmnessData.id().longValue());
          berryFirmness.setName(berryFirmnessData.name());
          LocalizationUtils.getLocalizationName(berryFirmnessData.names())
              .ifPresent(name -> berryFirmness.setName(name.name()));
          return berryFirmness;
        },
        berryFirmnessRepository::save,
        io.github.lishangbu.avalon.pokeapi.model.berry.BerryFirmness.class);
  }
}

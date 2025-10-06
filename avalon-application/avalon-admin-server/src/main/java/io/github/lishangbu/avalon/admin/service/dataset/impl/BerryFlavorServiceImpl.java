package io.github.lishangbu.avalon.admin.service.dataset.impl;

import io.github.lishangbu.avalon.admin.service.dataset.BerryFlavorService;
import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 树果风味服务实现
 *
 * <p>风味决定了宝可梦根据<a href="https://pokeapi.co/docs/v2#natures">性格</a>食用树果时是受益还是受损。详情可参考<a
 * href="http://bulbapedia.bulbagarden.net/wiki/Flavor">Bulbapedia</a>
 *
 * @author lishangbu
 * @since 2025/10/5
 */
@Service
@RequiredArgsConstructor
public class BerryFlavorServiceImpl implements BerryFlavorService {
  private final PokeApiService pokeApiService;

  private final BerryFlavorRepository berryFlavorRepository;

  @Override
  public List<BerryFlavor> importBerryFlavors() {
    return pokeApiService.importData(
        PokeDataTypeEnum.BERRY_FLAVOR,
        berryFlavorData -> {
          BerryFlavor berryFlavor = new BerryFlavor();
          berryFlavor.setInternalName(berryFlavorData.name());
          berryFlavor.setId(berryFlavorData.id().longValue());
          berryFlavor.setName(berryFlavorData.name());
          LocalizationUtils.getLocalizationName(berryFlavorData.names())
              .ifPresent(name -> berryFlavor.setName(name.name()));
          return berryFlavor;
        },
        berryFlavorRepository::save,
        io.github.lishangbu.avalon.pokeapi.model.berry.BerryFlavor.class);
  }
}

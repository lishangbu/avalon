package io.github.lishangbu.avalon.admin.service.dataset.impl;

import io.github.lishangbu.avalon.admin.service.dataset.BerryService;
import io.github.lishangbu.avalon.dataset.entity.Berry;
import io.github.lishangbu.avalon.dataset.repository.BerryRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Berry 服务实现
 *
 * @author lishangbu
 * @since 2025/10/4
 */
@Service
@RequiredArgsConstructor
public class BerryServiceImpl implements BerryService {

  private final BerryRepository berryRepository;

  private final PokeApiService pokeApiService;

  @Override
  public List<Berry> importBerries() {
    return pokeApiService.importData(
        PokeDataTypeEnum.BERRY,
        berryData -> {
          Berry berry = new Berry();
          berry.setInternalName(berryData.name());
          berry.setId(berryData.id().longValue());
          berry.setName(berryData.name());
          berry.setGrowthTime(berryData.growthTime());
          berry.setInternalName(berryData.name());
          berry.setSmoothness(berryData.smoothness());
          berry.setMaxHarvest(berryData.maxHarvest());
          berry.setSize(berryData.size());
          berry.setNaturalGiftPower(berryData.naturalGiftPower());
          berry.setSoilDryness(berryData.soilDryness());
          berry.setNaturalGiftTypeInternalName(berryData.naturalGiftType().name());
          berry.setFirmnessInternalName(berryData.firmness().name());
          return berry;
        },
        berryRepository::save,
        io.github.lishangbu.avalon.pokeapi.model.berry.Berry.class);
  }

  @Override
  public Page<Berry> getPageByCondition(Berry berry, Pageable pageable) {
    // TODO: 实现条件查询，当前为无条件分页
    return berryRepository.findAll(pageable);
  }

  @Override
  public Berry save(Berry berry) {
    return berryRepository.save(berry);
  }

  @Override
  public void removeById(Long id) {
    berryRepository.deleteById(id);
  }

  @Override
  public Berry update(Berry berry) {
    return berryRepository.save(berry);
  }
}

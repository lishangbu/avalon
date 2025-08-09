package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository;
import io.github.lishangbu.avalon.dataset.repository.BerryRepository;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * 树果数据集处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class BerryAttributeDataSetParseStrategy implements BasicDataSetParseStrategy {
  private final BerryRepository berryRepository;
  private final BerryFirmnessRepository berryFirmnessRepository;
  private final TypeRepository typeRepository;

  public BerryAttributeDataSetParseStrategy(
      BerryRepository berryRepository,
      BerryFirmnessRepository berryFirmnessRepository,
      TypeRepository typeRepository) {
    this.berryRepository = berryRepository;
    this.berryFirmnessRepository = berryFirmnessRepository;
    this.typeRepository = typeRepository;
  }

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource instanceof io.github.lishangbu.avalon.pokeapi.model.berry.Berry berryData) {
      Berry berry = new Berry();
      berry.setId(berryData.id());
      berry.setInternalName(berryData.name());
      // TODO ITEM还没有完成，先取返回的英文名顶一下
      berry.setName(berryData.name());
      berry.setSize(berryData.size());
      berry.setGrowthTime(berryData.growthTime());
      berry.setMaxHarvest(berryData.maxHarvest());
      berry.setSmoothness(berryData.smoothness());
      berry.setSoilDryness(berryData.soilDryness());
      berry.setNaturalGiftPower(berryData.naturalGiftPower());
      typeRepository
          .findByInternalName(berryData.naturalGiftType().name())
          .ifPresent(berry::setNaturalGiftType);
      berryFirmnessRepository
          .findByInternalName(berryData.firmness().name())
          .ifPresent(berry::setFirmness);
      return berry;
    }
    return null;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.BERRY;
  }

  @Override
  public JpaRepository getRepository() {
    return this.berryRepository;
  }
}

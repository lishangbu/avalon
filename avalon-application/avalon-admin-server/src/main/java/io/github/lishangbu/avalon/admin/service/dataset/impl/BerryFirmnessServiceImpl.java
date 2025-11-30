package io.github.lishangbu.avalon.admin.service.dataset.impl;

import io.github.lishangbu.avalon.admin.service.dataset.BerryFirmnessService;
import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  @Override
  public Page<BerryFirmness> getPageByCondition(BerryFirmness berryFirmness, Pageable pageable) {
    return berryFirmnessRepository.findAll(
        Example.of(
            berryFirmness,
            ExampleMatcher.matching()
              .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreNullValues()),
        pageable);
  }

  /**
   * 根据条件查询树果坚硬度列表
   *
   * <p>支持按 name/internalName 模糊查询，其余字段精确匹配
   *
   * @param berryFirmness 查询条件，支持部分字段模糊查询
   * @return 树果坚硬度列表
   */
  @Override
  public List<BerryFirmness> listByCondition(BerryFirmness berryFirmness) {
    ExampleMatcher matcher =
        ExampleMatcher.matching()
            .withIgnoreNullValues()
          .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
    return berryFirmnessRepository.findAll(Example.of(berryFirmness, matcher));
  }

  @Override
  public BerryFirmness save(BerryFirmness berryFirmness) {
    return berryFirmnessRepository.save(berryFirmness);
  }

  @Override
  public BerryFirmness update(BerryFirmness berryFirmness) {
    return berryFirmnessRepository.save(berryFirmness);
  }

  @Override
  public void removeById(Long id) {
    berryFirmnessRepository.deleteById(id);
  }
}

package io.github.lishangbu.avalon.pokeapi.service.impl;

import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFirmness;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.service.BerryFirmnessService;
import java.io.Serializable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 树果硬度服务实现
 *
 * @author lishangbu
 * @since 2025/5/22
 */
@Service
public class BerryFirmnessServiceImpl extends AbstractPokeApiService
    implements BerryFirmnessService {

  @Cacheable(value = "berryFirmnesses", key = "#offset + '-' + #limit")
  @Override
  public NamedAPIResourceList listBerryFirmnesses(Integer offset, Integer limit) {
    return listNamedAPIResources("/berry-firmness", offset, limit);
  }

  @Cacheable(value = "berryFirmness", key = "#arg")
  @Override
  public BerryFirmness getBerryFirmness(Serializable arg) {
    return getEntityFromUri(BerryFirmness.class, "/berry-firmness/{arg}", arg);
  }
}

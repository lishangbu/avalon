package io.github.lishangbu.avalon.pokeapi.service.impl;

import io.github.lishangbu.avalon.pokeapi.model.berry.Berry;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.service.BerryService;
import java.io.Serializable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 树果服务实现
 *
 * @author lishangbu
 * @since 2025/5/22
 */
@Service
public class BerryServiceImpl extends AbstractPokeApiService implements BerryService {
  @Cacheable(value = "berries", key = "#offset + '-' + #limit")
  @Override
  public NamedAPIResourceList listBerries(Integer offset, Integer limit) {
    return listNamedAPIResources("/berry", offset, limit);
  }

  @Override
  @Cacheable(value = "berry", key = "#arg")
  public Berry getBerry(Serializable arg) {
    return getEntityFromUri(Berry.class, "/berry/{arg}", arg);
  }
}

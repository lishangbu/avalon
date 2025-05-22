package io.github.lishangbu.avalon.pokeapi.service.impl;

import io.github.lishangbu.avalon.pokeapi.model.berry.Berry;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type;
import io.github.lishangbu.avalon.pokeapi.service.BerryService;
import io.github.lishangbu.avalon.pokeapi.service.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.service.TypeService;
import java.io.Serializable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * PokeApi请求服务聚合实现
 *
 * @author lishangbu
 * @since 2025/5/21
 */
@Service
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class PokeApiServiceImpl implements PokeApiService {
  @Autowired private BerryService berryService;

  @Autowired private TypeService typeService;

  @Override
  public NamedAPIResourceList listTypes(Integer offset, Integer limit) {
    return typeService.listTypes(offset, limit);
  }

  @Override
  public Type getType(Serializable arg) {
    return typeService.getType(arg);
  }

  @Override
  public NamedAPIResourceList listBerries(Integer offset, Integer limit) {
    return berryService.listBerries(offset, limit);
  }

  @Override
  public Berry getBerry(Serializable arg) {
    return berryService.getBerry(arg);
  }
}

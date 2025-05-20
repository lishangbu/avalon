package io.github.lishangbu.avalon.pokeapi.service;

import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type;
import java.io.Serializable;

/**
 * Poke API 请求模板
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public interface PokeApiTemplate {
  NamedAPIResourceList listTypes(Integer offset, Integer limit);

  Type getType(Serializable arg);
}

package io.github.lishangbu.avalon.shell.pokeapi.service;

import io.github.lishangbu.avalon.shell.pokeapi.model.PokeApiResource;
import io.github.lishangbu.avalon.shell.pokeapi.model.PokeApiTypeDetailResult;

import java.util.List;

/**
 * Poke API属性服务
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public interface PokeApiTypeService {

  List<PokeApiResource> listPokeApiTypes();

  PokeApiTypeDetailResult getPokeApiTypeDetailByArg(String arg);

}

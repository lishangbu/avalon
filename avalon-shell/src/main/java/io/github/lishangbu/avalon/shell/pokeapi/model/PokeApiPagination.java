package io.github.lishangbu.avalon.shell.pokeapi.model;

import java.util.List;

/**
 * poke-api分页结果
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record PokeApiPagination<T>(
  int count,
  String next,
  String previous,
  List<T> results
) {

}

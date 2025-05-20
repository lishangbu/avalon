package io.github.lishangbu.avalon.shell.pokeapi.model;

import java.util.List;

/**
 * @author lishangbu
 * @since 2025/5/20
 */
public record PokeApiTypeDetailResult(
  Integer id,
  String name,
  List<Name> names
) {
  public record Name(PokeApiResource language, String name) {

  }

}

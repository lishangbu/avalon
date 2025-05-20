package io.github.lishangbu.avalon.shell.pokeapi.model;

/**
 * poke-api资源结果，通常包含name和URL
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record PokeApiResource(
  String name,
  String url
) {

}

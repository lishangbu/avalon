package io.github.lishangbu.avalon.pokeapi.model.pokemon;

/**
 * 宝可梦的叫声
 *
 * @param latest 这个宝可梦叫声的最新描述
 * @param legacy 这个宝可梦叫声的传统描述
 * @author lishangbu
 * @since 2025/6/8
 */
public record PokemonCries(String latest, String legacy) {}

package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Version;

/**
 * 宝可梦在特定版本中持有道具的信息
 *
 * @param version 持有道具的游戏版本{@link Version}
 * @param rarity 道具被持有的频率
 * @author lishangbu
 * @see Version
 * @since 2025/6/8
 */
public record PokemonHeldItemVersion(NamedApiResource<Version> version, Integer rarity) {}

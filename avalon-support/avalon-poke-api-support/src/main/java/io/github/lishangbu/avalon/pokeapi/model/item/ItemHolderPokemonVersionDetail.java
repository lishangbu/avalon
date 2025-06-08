package io.github.lishangbu.avalon.pokeapi.model.item;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Version;

/**
 * 持有道具的宝可梦在特定游戏版本中的详细信息
 *
 * @param rarity 该宝可梦在此版本中持有此道具的频率
 * @param version 宝可梦持有此道具的游戏版本{@link Version}
 * @author lishangbu
 * @see Version
 * @since 2025/5/24
 */
public record ItemHolderPokemonVersionDetail(Integer rarity, NamedApiResource<Version> version) {}

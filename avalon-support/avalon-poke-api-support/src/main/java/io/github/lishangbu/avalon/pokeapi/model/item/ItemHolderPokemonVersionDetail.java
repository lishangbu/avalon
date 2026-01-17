package io.github.lishangbu.avalon.pokeapi.model.item;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Version;

/// 持有道具的宝可梦在特定版本中的详情
///
/// @param rarity  在该版本中持有此道具的频率
/// @param version 游戏版本引用
/// @author lishangbu
/// @see Version
/// @since 2025/5/24
public record ItemHolderPokemonVersionDetail(Integer rarity, NamedApiResource<Version> version) {}

package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Version;

/// 宝可梦持有道具的版本信息模型
///
/// 表示在特定游戏版本中宝可梦持有道具的频率与相关版本
///
/// @param version 游戏版本引用
/// @param rarity  道具被持有的频率
/// @author lishangbu
/// @see Version
/// @since 2025/6/8
public record PokemonHeldItemVersion(NamedApiResource<Version> version, Integer rarity) {}

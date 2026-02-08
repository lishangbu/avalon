package io.github.lishangbu.avalon.pokeapi.model.pokemon;

/// 宝可梦的叫声模型
///
/// @param latest 最新描述
/// @param legacy 传统描述
/// @author lishangbu
/// @since 2025/6/8
public record PokemonCries(String latest, String legacy) {}

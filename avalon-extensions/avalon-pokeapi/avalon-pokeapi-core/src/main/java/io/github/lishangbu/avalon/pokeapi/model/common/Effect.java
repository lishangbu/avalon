package io.github.lishangbu.avalon.pokeapi.model.common;

/// 效果文本模型（参考 PokeAPI）
///
/// 参考 [PokeAPI](https://pokeapi.co/docs/v2) 的 Utility/Common Models/Effect
///
/// @param effect   特定语言中的本地化效果文本
/// @param language 文本所使用的语言引用
/// @author lishangbu
/// @since 2025/5/20
public record Effect(String effect, NamedApiResource language) {}

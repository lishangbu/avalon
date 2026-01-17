package io.github.lishangbu.avalon.pokeapi.model.common;

/// PokeAPI 的 APIResource 通用模型
///
/// 表示引用资源的 URL
///
/// @param url 引用资源的 URL
/// @author lishangbu
/// @since 2025/5/20
public record APIResource<T>(String url) {}

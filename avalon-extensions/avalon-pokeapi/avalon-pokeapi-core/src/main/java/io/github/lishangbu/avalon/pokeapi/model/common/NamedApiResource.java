package io.github.lishangbu.avalon.pokeapi.model.common;

/// 命名 API 资源引用模型
///
/// @param name 引用资源的名称
/// @param url  引用资源的 URL
/// @author lishangbu
/// @since 2025/5/20
public record NamedApiResource<T>(String name, String url) {}

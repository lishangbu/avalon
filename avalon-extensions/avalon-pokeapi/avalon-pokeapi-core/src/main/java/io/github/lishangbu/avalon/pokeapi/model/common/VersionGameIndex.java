package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/// PokeAPI 版本游戏索引模型
///
/// @param gameIndex 该 API 资源在游戏数据中的内部 ID
/// @param version   与此游戏索引相关的版本
/// @author lishangbu
/// @since 2025/5/20
public record VersionGameIndex(
        @JsonProperty("game_index") Integer gameIndex, NamedApiResource<?> version) {}

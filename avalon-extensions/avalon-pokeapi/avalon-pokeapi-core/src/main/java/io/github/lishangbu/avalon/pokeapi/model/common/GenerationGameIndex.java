package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/// 代别游戏索引模型
///
/// @param gameIndex  游戏数据中该 API 资源的内部 ID
/// @param generation 与该游戏索引相关的世代引用
/// @author lishangbu
/// @since 2025/5/20
public record GenerationGameIndex(
        @JsonProperty("game_index") Integer gameIndex, NamedApiResource<?> generation) {}

package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/VersionGameIndex</a>
 *
 * @param gameIndex 该API资源在游戏数据中的内部ID
 * @param version 与此游戏索引相关的版本
 * @author lishangbu
 * @since 2025/5/20
 */
public record VersionGameIndex(
    @JsonProperty("game_index") Integer gameIndex, NamedApiResource<?> version) {}

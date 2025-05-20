package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/VersionGameIndex</a>
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record VersionGameIndex(
    @JsonProperty("game_index") Integer gameIndex, NamedApiResource<?> version) {

  /** 获取该API资源在游戏数据中的内部ID */
  public Integer gameIndex() {
    return gameIndex;
  }

  /** 获取与此游戏索引相关的版本 */
  public NamedApiResource<?> version() {
    return version;
  }
}

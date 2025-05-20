package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/GenerationGameIndex</a>
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record GenerationGameIndex(
    @JsonProperty("game_index") Integer gameIndex, NamedApiResource<?> generation) {

  /** 获取游戏数据中该API资源的内部ID */
  public Integer gameIndex() {
    return gameIndex;
  }

  /** 获取与该游戏索引相关的代数 */
  public NamedApiResource<?> generation() {
    return generation;
  }
}

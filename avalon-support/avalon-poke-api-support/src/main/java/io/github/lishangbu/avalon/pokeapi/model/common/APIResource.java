package io.github.lishangbu.avalon.pokeapi.model.common;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/APIResource</a>
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record APIResource<T>(String url) {
  /** 引用资源的 URL */
  public String url() {
    return url;
  }
}

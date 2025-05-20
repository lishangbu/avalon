package io.github.lishangbu.avalon.pokeapi.model.common;

import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/VerboseEffect</a>
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record VerboseEffect(
    String effect, String shortEffect, NamedApiResource<Language> language) {

  /** 获取该API资源在特定语言中的本地化效果文本 */
  public String effect() {
    return effect;
  }

  /** 获取该API资源的简短本地化效果文本 */
  public String shortEffect() {
    return shortEffect;
  }

  /** 获取该效果文本所使用的语言 */
  public NamedApiResource<Language> language() {
    return language;
  }
}

package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/VersionGroupFlavorText</a>
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record VersionGroupFlavorText(
    String text,
    NamedApiResource<Language> language,
    @JsonProperty("version_group") NamedApiResource<?> versionGroup) {
  /** 获取该API资源在特定语言中的本地化名称 */
  public String text() {
    return text;
  }

  /** 获取该名称所使用的语言 */
  public NamedApiResource<Language> language() {
    return language;
  }

  /** 获取使用此偏好文本的版本组 */
  public NamedApiResource<?> versionGroup() {
    return versionGroup;
  }
}

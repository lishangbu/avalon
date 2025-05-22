package io.github.lishangbu.avalon.pokeapi.model.contest;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">Contests/Contest Types/ContestName (type)</a>
 *
 * @author lishangbu
 * @since 2025/5/22
 */
public record ContestName(String name, String color, NamedApiResource<Language> language) {

  /** 竞赛的名称 */
  public String name() {
    return name;
  }

  /** 与竞赛名称关联的颜色 */
  public String color() {
    return color;
  }

  /** 名称所使用的语言 */
  public NamedApiResource<Language> language() {
    return language;
  }
}

package io.github.lishangbu.avalon.pokeapi.model.language;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import java.util.List;

/**
 * 语言信息，用于API资源信息的多语言翻译
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record Language(
    Integer id, String name, Boolean official, String iso639, String iso3166, List<Name> names) {
  /** 该资源的标识符 */
  public Integer id() {
    return id;
  }

  /** 该资源的名称 */
  public String name() {
    return name;
  }

  /** 游戏是否以此语言发布 */
  public Boolean official() {
    return official;
  }

  /** 该语言使用的国家的两字母代码（注意，这不是唯一的） */
  public String iso639() {
    return iso639;
  }

  /** 该语言的两字母代码（注意，这不是唯一的） */
  public String iso3166() {
    return iso3166;
  }

  /** 该资源在不同语言中的名称 */
  public List<Name> names() {
    return names;
  }
}

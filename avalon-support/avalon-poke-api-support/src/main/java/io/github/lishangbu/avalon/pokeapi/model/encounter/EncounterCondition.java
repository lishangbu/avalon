package io.github.lishangbu.avalon.pokeapi.model.encounter;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 影响野外出现宝可梦的条件，例如白天或夜晚，详情可参考<a
 * href="https://bulbapedia.bulbagarden.net/wiki/Wild_Pok%C3%A9mon">Bulbapedia</a>
 *
 * @author lishangbu
 * @since 2025/5/23
 */
public record EncounterCondition(
    Integer id,
    String name,
    List<Name> names,
    List<NamedApiResource<EncounterConditionValue>> values) {
  /** 该资源的唯一标识符 */
  public Integer id() {
    return id;
  }

  /** 该资源的名称 */
  public String name() {
    return name;
  }

  /** 该资源在不同语言下的名称 */
  public List<Name> names() {
    return names;
  }

  /** 该遭遇条件的所有可能取值列表 */
  public List<NamedApiResource<EncounterConditionValue>> values() {
    return values;
  }
}

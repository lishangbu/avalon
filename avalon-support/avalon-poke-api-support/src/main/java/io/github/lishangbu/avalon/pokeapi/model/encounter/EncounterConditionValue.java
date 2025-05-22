package io.github.lishangbu.avalon.pokeapi.model.encounter;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 遭遇条件值是遭遇条件可以拥有的各种状态，例如一天中的时间可以是白天或夜晚，详情可参考Bulbapedia
 *
 * @author lishangbu
 * @since 2025/5/23
 */
public record EncounterConditionValue(
    Integer id, String name, NamedApiResource<EncounterCondition> condition, List<Name> names) {
  /** 该资源的唯一标识符 */
  public Integer id() {
    return id;
  }

  /** 该资源的名称 */
  public String name() {
    return name;
  }

  /** 该遭遇条件值所属的遭遇条件 */
  public NamedApiResource<EncounterCondition> condition() {
    return condition;
  }

  /** 该资源在不同语言下的名称 */
  public List<Name> names() {
    return names;
  }
}

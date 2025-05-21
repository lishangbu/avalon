package io.github.lishangbu.avalon.pokeapi.model.berry;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">Berries/Berry Flavors/BerryFlavor (type)</a>
 *
 * @author lishangbu
 * @since 2025/5/21
 */
public record BerryFlavor(
    Integer id,
    String name,
    List<FlavorBerryMap> berries,
    @JsonProperty("contest_type") List<NamedApiResource<?>> contestType,
    List<Name> names) {
  /** 该资源的标识符 */
  public Integer id() {
    return id;
  }

  /** 该资源的名称 */
  public String name() {
    return name;
  }

  /** 具有该风味的树果列表 */
  public List<FlavorBerryMap> berries() {
    return berries;
  }

  /** 与该树果风味相关的对战类型 */
  public List<NamedApiResource<?>> contestType() {
    return contestType;
  }

  /** 该资源在不同语言下的名称 */
  public List<Name> names() {
    return names;
  }
}

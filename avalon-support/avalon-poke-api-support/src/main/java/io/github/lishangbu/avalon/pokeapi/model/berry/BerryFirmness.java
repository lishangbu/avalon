package io.github.lishangbu.avalon.pokeapi.model.berry;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * Berries can be soft or hard. Check out<a
 * href="https://bulbapedia.bulbagarden.net/wiki/Category:Berries_by_firmness">Bulbapedia</a> for
 * greater detail.
 *
 * <p>参考<a href="https://pokeapi.co/docs/v2">Berries/Berry Firmnesses/Berry Firmnesses(type)</a>
 *
 * @author lishangbu
 * @since 2025/5/21
 */
public record BerryFirmness(
    Integer id, String name, List<NamedApiResource<Berry>> berries, List<Name> names) {
  /** 该资源的标识符 */
  public Integer id() {
    return id;
  }

  /** 该资源的名称 */
  public String name() {
    return name;
  }

  /** 具有该硬度的树果列表 */
  public List<NamedApiResource<Berry>> berries() {
    return berries;
  }

  /** 该资源在不同语言下的名称 */
  public List<Name> names() {
    return names;
  }
}

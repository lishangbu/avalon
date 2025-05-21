package io.github.lishangbu.avalon.pokeapi.model.berry;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">Berries/Berry/BerryFlavorMap (type)</a>
 *
 * @author lishangbu
 * @since 2025/5/21
 */
public record BerryFlavorMap(Integer potency, NamedApiResource<Berry> berry) {
  /** 该风味对该树果的影响力 */
  public Integer potency() {
    return potency;
  }

  /** 具有该风味的树果 */
  public NamedApiResource<Berry> berry() {
    return berry;
  }
}

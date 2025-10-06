package io.github.lishangbu.avalon.pokeapi.model.berry;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">Berries/Berry Flavors/FlavorBerryMap (type)</a>
 *
 * @param potency 该风味对该树果的影响力
 * @param berry 具有该风味的树果{@link Berry}
 * @author lishangbu
 * @see Berry
 * @since 2025/5/21
 */
public record FlavorBerryMap(Integer potency, NamedApiResource berry) {}

package io.github.lishangbu.avalon.pokeapi.model.berry;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 树果可以是柔软的，也可以是坚硬的。详情可参考<a
 * href="https://bulbapedia.bulbagarden.net/wiki/Category:Berries_by_firmness">Bulbapedia</a>
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param berries 具有该硬度的树果列表
 * @param names 该资源在不同语言下的名称
 * @author lishangbu
 * @since 2025/5/21
 */
public record BerryFirmness(
    Integer id, String name, List<NamedApiResource<Berry>> berries, List<Name> names) {}

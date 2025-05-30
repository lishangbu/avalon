package io.github.lishangbu.avalon.pokeapi.model.contest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFlavor;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 竞赛类型是评委在宝可梦华丽大赛中用于衡量宝可梦状态的类别。详情可参考<a
 * href="http://bulbapedia.bulbagarden.net/wiki/Contest_condition">Bulbapedia</a>
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param berryFlavor 与该竞赛类型相关的树果口味{@link BerryFlavor}
 * @param names 该竞赛类型在不同语言下的名称{@link ContestName}
 * @author lishangbu
 * @see BerryFlavor
 * @see ContestName
 * @since 2025/5/22
 */
public record ContestType(
    Integer id,
    String name,
    @JsonProperty("berry_flavor") NamedApiResource<BerryFlavor> berryFlavor,
    List<ContestName> names) {}

package io.github.lishangbu.avalon.pokeapi.model.berry;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;
import java.util.List;

/**
 * 树果是可以为宝可梦恢复HP和异常状态、提升能力，甚至在食用时抵消伤害的小型果实。详情可参考<a
 * href="http://bulbapedia.bulbagarden.net/wiki/Berry">Bulbapedia</a>
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param growthTime 树生长到下一个阶段所需的时间（小时）
 * @param maxHarvest 第四世代中一棵树上最多可生长的该树果数量
 * @param naturalGiftPower 搭配该树果使用"自然之恩"招式时的威力
 * @param size 该树果的大小（毫米）
 * @param smoothness 该树果的光滑度，用于制作宝可方块或宝芬
 * @param soilDryness 树果生长时使土壤干燥的速度，数值越高土壤干燥越快
 * @param firmness 该树果的硬度{@link BerryFirmness}，用于制作宝可方块或宝芬
 * @param flavors 该树果的风味及其对应的强度列表{@link FlavorBerryMap}
 * @param item 该树果对应的道具{@link Item}数据
 * @param naturalGiftType 搭配该树果使用"自然之恩"招式时继承的属性类型
 * @author lishangbu
 * @see BerryFirmness
 * @see Item
 * @since 2025/5/21
 */
public record Berry(
    Integer id,
    String name,
    @JsonProperty("growth_time") Integer growthTime,
    @JsonProperty("max_harvest") Integer maxHarvest,
    @JsonProperty("natural_gift_power") Integer naturalGiftPower,
    Integer size,
    Integer smoothness,
    @JsonProperty("soil_dryness") Integer soilDryness,
    NamedApiResource firmness,
    List<FlavorBerryMap> flavors,
    NamedApiResource item,
    @JsonProperty("natural_gift_type") NamedApiResource<Type> naturalGiftType) {}

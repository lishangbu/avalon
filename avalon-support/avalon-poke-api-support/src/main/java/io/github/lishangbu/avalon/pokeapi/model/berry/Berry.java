package io.github.lishangbu.avalon.pokeapi.model.berry;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type;
import java.util.List;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">Berries/Berries/Berry (type)</a>
 *
 * @author lishangbu
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
    NamedApiResource<BerryFirmness> firmness,
    List<FlavorBerryMap> flavors,
    NamedApiResource<?> item,
    @JsonProperty("natural_gift_type") NamedApiResource<Type> type) {
  /** 该资源的标识符 */
  public Integer id() {
    return id;
  }

  /** 该资源的名称 */
  public String name() {
    return name;
  }

  /** 树生长到下一个阶段所需的时间（小时） */
  public Integer growthTime() {
    return growthTime;
  }

  /** 第四世代中一棵树上最多可生长的该树果数量 */
  public Integer maxHarvest() {
    return maxHarvest;
  }

  /** 搭配该树果使用“自然之恩”招式时的威力 */
  public Integer naturalGiftPower() {
    return naturalGiftPower;
  }

  /** 该树果的大小（毫米） */
  public Integer size() {
    return size;
  }

  /** 该树果的光滑度，用于制作宝可方块或宝芬 */
  public Integer smoothness() {
    return smoothness;
  }

  /** 树果生长时使土壤干燥的速度，数值越高土壤干燥越快 */
  public Integer soilDryness() {
    return soilDryness;
  }

  /** 该树果的硬度，用于制作宝可方块或宝芬 */
  public NamedApiResource<BerryFirmness> firmness() {
    return firmness;
  }

  /** 该树果的风味及其对应的强度列表 */
  public List<FlavorBerryMap> flavors() {
    return flavors;
  }

  /** 该树果对应的道具数据 */
  public NamedApiResource<?> item() {
    return item;
  }

  /** 搭配该树果使用“自然之恩”招式时继承的属性类型 */
  public NamedApiResource<Type> type() {
    return type;
  }
}

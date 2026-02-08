package io.github.lishangbu.avalon.pokeapi.model.berry;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;
import java.util.List;

/// 树果模型
///
/// 表示树果的游戏数据，包括生长时间、口味、对应道具与相关属性等
///
/// @param id               资源标识符
/// @param name             资源名称
/// @param growthTime       生长到下一个阶段所需时间（小时）
/// @param maxHarvest       第四世代中一棵树上最多可收获的数量
/// @param naturalGiftPower 自然之恩招式的威力
/// @param size             大小（毫米）
/// @param smoothness       光滑度
/// @param soilDryness      土壤干燥速度
/// @param firmness         树果硬度引用
/// @param flavors          树果风味及强度列表
/// @param item             对应的道具引用
/// @param naturalGiftType  自然之恩招式继承的属性类型引用
/// @see BerryFirmness
/// @see Item
/// @since 2025/5/21
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
    List<BerryFlavorMap> flavors,
    NamedApiResource item,
    @JsonProperty("natural_gift_type") NamedApiResource<Type> naturalGiftType) {}

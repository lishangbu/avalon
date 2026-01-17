package io.github.lishangbu.avalon.pokeapi.model.contest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFlavor;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 竞赛类型模型（ContestType）
///
/// @param id          资源标识符
/// @param name        资源名称
/// @param berryFlavor 与该竞赛类型相关的树果口味引用
/// @param names       该竞赛类型在不同语言下的名称列表
/// @author lishangbu
/// @see BerryFlavor
/// @see ContestName
/// @since 2025/5/22
public record ContestType(
    Integer id,
    String name,
    @JsonProperty("berry_flavor") NamedApiResource<BerryFlavor> berryFlavor,
    List<ContestName> names) {}

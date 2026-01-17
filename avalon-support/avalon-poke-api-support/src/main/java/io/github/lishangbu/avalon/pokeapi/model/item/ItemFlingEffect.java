package io.github.lishangbu.avalon.pokeapi.model.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Effect;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 道具投掷效果模型
///
/// 表示将道具用于投掷（fling）时的效果与对应道具列表
///
/// @param id            资源标识符
/// @param name          资源名称
/// @param effectEntries 使用不同语言列出的投掷效果描述
/// @param items         具有此投掷效果的道具引用列表
/// @author lishangbu
/// @see Effect
/// @see Item
/// @since 2025/5/24
public record ItemFlingEffect(
    Integer id,
    String name,
    @JsonProperty("effect_entries") List<Effect> effectEntries,
    List<NamedApiResource<Item>> items) {}

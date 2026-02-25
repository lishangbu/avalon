package io.github.lishangbu.avalon.pokeapi.model.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.*;
import java.util.List;

/// 道具模型
///
/// 表示游戏中的道具及其元数据，如价格、投掷效果、所属类别与本地化文本等
///
/// @param id                资源标识符
/// @param name              资源名称
/// @param cost              商店价格
/// @param flingPower        投掷时的威力
/// @param flingEffect       投掷时的效果引用
/// @param attributes        道具属性列表
/// @param category          道具类别引用
/// @param effectEntries     不同语言的效果描述
/// @param flavorTextEntries 不同语言的风味文本
/// @param gameIndices       与道具相关的游戏索引按世代列出
/// @param names             不同语言的名称列表
/// @param sprites           道具精灵图集
/// @param heldByPokemon     可能持有此道具的宝可梦列表
/// @param babyTriggerFor    繁殖时触发婴儿孵化用的引用
/// @param machines          与此道具相关的机器列表
/// @author lishangbu
/// @see ItemFlingEffect
/// @see ItemAttribute
/// @see ItemCategory
/// @see VerboseEffect
/// @see VersionGroupFlavorText
/// @see GenerationGameIndex
/// @see Name
/// @see ItemSprites
/// @see ItemHolderPokemon
/// @since 2025/5/24
public record Item(
        Integer id,
        String name,
        Integer cost,
        @JsonProperty("fling_power") Integer flingPower,
        @JsonProperty("fling_effect") NamedApiResource flingEffect,
        List<NamedApiResource> attributes,
        NamedApiResource<ItemCategory> category,
        @JsonProperty("effect_entries") List<VerboseEffect> effectEntries,
        @JsonProperty("flavor_text_entries") List<VersionGroupFlavorText> flavorTextEntries,
        @JsonProperty("game_indices") List<GenerationGameIndex> gameIndices,
        List<Name> names,
        ItemSprites sprites,
        @JsonProperty("held_by_pokemon") List<ItemHolderPokemon> heldByPokemon,
        @JsonProperty("baby_trigger_for") APIResource<?> babyTriggerFor,
        List<?> machines) {}

package io.github.lishangbu.avalon.pokeapi.model.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 图鉴宝可梦条目模型
///
/// 表示宝可梦物种在图鉴中的索引信息
///
/// @param entryNumber    图鉴中的索引号
/// @param pokemonSpecies 被引用的宝可梦物种
/// @author lishangbu
/// @since 2025/5/24
public record PokemonEntry(
        @JsonProperty("entry_number") Integer entryNumber,
        @JsonProperty("pokemon_species") NamedApiResource<?> pokemonSpecies) {}

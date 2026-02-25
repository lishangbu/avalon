package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Pokedex;

/// 宝可梦种类图鉴条目模型
///
/// 表示某宝可梦种类在指定图鉴中的索引记录
///
/// @param entryNumber 图鉴中的索引编号
/// @param pokedex     可在其中找到该种类的图鉴引用
/// @author lishangbu
/// @see Pokedex
/// @since 2025/6/8
public record PokemonSpeciesDexEntry(
        @JsonProperty("entry_number") Integer entryNumber, NamedApiResource<Pokedex> pokedex) {}

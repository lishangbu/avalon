package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 宝可梦种类变种表示同一个种类的宝可梦的不同形式
 *
 * @param isDefault 这个变种是否是默认变种
 * @param pokemon 宝可梦{@link Pokemon}变种
 * @author lishangbu
 * @see Pokemon
 * @since 2025/6/8
 */
public record PokemonSpeciesVariety(
    @JsonProperty("is_default") Boolean isDefault, NamedApiResource<Pokemon> pokemon) {}

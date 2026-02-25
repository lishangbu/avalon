package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;

/// 宝可梦形态的精灵图像
///
/// @param frontDefault 战斗中正面的默认图像
/// @param frontShiny   战斗中正面的闪光图像
/// @param backDefault  战斗中背面的默认图像
/// @param backShiny    战斗中背面的闪光图像
/// @author lishangbu
/// @since 2025/6/8
public record PokemonFormSprites(
        @JsonProperty("front_default") String frontDefault,
        @JsonProperty("front_shiny") String frontShiny,
        @JsonProperty("back_default") String backDefault,
        @JsonProperty("back_shiny") String backShiny) {}

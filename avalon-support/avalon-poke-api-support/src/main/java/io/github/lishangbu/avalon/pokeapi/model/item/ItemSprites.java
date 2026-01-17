package io.github.lishangbu.avalon.pokeapi.model.item;

import com.fasterxml.jackson.annotation.JsonProperty;

/// 道具图像
///
/// @param defaultSprite 默认图像 URL
/// @author lishangbu
/// @since 2025/5/24
public record ItemSprites(@JsonProperty("default") String defaultSprite) {}

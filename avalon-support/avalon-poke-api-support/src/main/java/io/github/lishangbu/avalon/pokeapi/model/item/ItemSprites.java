package io.github.lishangbu.avalon.pokeapi.model.item;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 物品图像
 *
 * @param defaultSprite The default depiction of this item.
 * @author lishangbu
 * @since 2025/5/24
 */
public record ItemSprites(@JsonProperty("default") String defaultSprite) {}

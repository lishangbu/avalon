package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import java.util.List;

/**
 * 宝可梦持有的道具信息
 *
 * @param item 宝可梦持有的道具{@link Item}
 * @param versionDetails 在不同版本中持有该道具的详细信息{@link PokemonHeldItemVersion}
 * @author lishangbu
 * @see Item
 * @see PokemonHeldItemVersion
 * @since 2025/6/8
 */
public record PokemonHeldItem(
    NamedApiResource<Item> item,
    @JsonProperty("version_details") List<PokemonHeldItemVersion> versionDetails) {}

package io.github.lishangbu.avalon.pokeapi.model.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 持有特定物品的宝可梦及相关信息
 *
 * @param pokemon 持有此物品的宝可梦
 * @param versionDetails 宝可梦持有此物品的版本详细信息{@link ItemHolderPokemonVersionDetail}
 * @author lishangbu
 * @see ItemHolderPokemonVersionDetail
 * @since 2025/5/24
 */
public record ItemHolderPokemon(
    NamedApiResource<?> pokemon,
    @JsonProperty("version_details") List<ItemHolderPokemonVersionDetail> versionDetails) {}

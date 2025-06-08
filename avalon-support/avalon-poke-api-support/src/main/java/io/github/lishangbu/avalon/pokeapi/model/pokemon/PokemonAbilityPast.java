package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import java.util.List;

/**
 * 宝可梦在过去世代中的特性
 *
 * @param generation 所列特性是宝可梦拥有的最后一个世代{@link Generation}
 * @param abilities 宝可梦在所列世代及之前拥有的特性{@link PokemonAbility}列表。如果为null，则表示该位置之前为空
 * @author lishangbu
 * @see Generation
 * @see PokemonAbility
 * @since 2025/6/8
 */
public record PokemonAbilityPast(
    NamedApiResource<Generation> generation, List<PokemonAbility> abilities) {}

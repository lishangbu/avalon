package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import java.util.List;

/**
 * 宝可梦在过去世代中的属性
 *
 * @param generation 所列属性是宝可梦拥有的最后一个世代
 * @param types 宝可梦在所列世代及之前拥有的属性列表
 * @author lishangbu
 * @since 2025/6/8
 */
public record PokemonTypePast(NamedApiResource<Generation> generation, List<PokemonType> types) {}

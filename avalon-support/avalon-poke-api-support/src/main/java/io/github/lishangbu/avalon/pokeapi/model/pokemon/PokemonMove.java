package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.move.Move;
import java.util.List;

/**
 * 宝可梦可以学习的技能
 *
 * @param move 宝可梦可以学习的技能{@link Move}
 * @param versionGroupDetails 宝可梦可以学习该技能的游戏版本详情{@link PokemonMoveVersion}
 * @author lishangbu
 * @see Move
 * @see PokemonMoveVersion
 * @since 2025/6/8
 */
public record PokemonMove(
    NamedApiResource<Move> move,
    @JsonProperty("version_group_details") List<PokemonMoveVersion> versionGroupDetails) {}

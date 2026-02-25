package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.move.Move;
import java.util.List;

/// 宝可梦可学习的招式模型
///
/// 表示宝可梦可以学习的技能及其在不同版本组下的学习详情
///
/// @param move                招式引用
/// @param versionGroupDetails 学习此招式的版本组详情列表
/// @author lishangbu
/// @see Move
/// @see PokemonMoveVersion
/// @since 2025/6/8
public record PokemonMove(
        NamedApiResource<Move> move,
        @JsonProperty("version_group_details") List<PokemonMoveVersion> versionGroupDetails) {}

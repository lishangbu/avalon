package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveLearnMethod;

/// 宝可梦在特定版本中学习技能的方式
///
/// @param moveLearnMethod 学习技能的方法
/// @param versionGroup    学习技能的游戏版本组
/// @param levelLearnedAt  学习技能的最低等级
/// @param order           学习顺序（新技能会替换顺序值最低的技能）
/// @author lishangbu
/// @since 2025/6/8
public record PokemonMoveVersion(
        @JsonProperty("move_learn_method") NamedApiResource<MoveLearnMethod> moveLearnMethod,
        @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup,
        @JsonProperty("level_learned_at") Integer levelLearnedAt,
        Integer order) {}

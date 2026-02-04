package io.github.lishangbu.avalon.pokeapi.model.location;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Version;

/// 遭遇版本详情
///
/// @param rate    遭遇几率
/// @param version 可以以给定几率遇到的游戏版本引用
/// @author lishangbu
/// @since 2025/5/26
public record EncounterVersionDetails(Integer rate, NamedApiResource<Version> version) {}

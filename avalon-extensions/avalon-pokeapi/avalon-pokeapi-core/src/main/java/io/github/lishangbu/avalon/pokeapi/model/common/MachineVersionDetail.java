package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/// 机器版本详情（参考 PokeAPI）
///
/// 参考 [PokeAPI](https://pokeapi.co/docs/v2) 的 Utility/Common Models/MachineVersionDetail
///
/// @param machine      教导技能的机器对应的道具引用
/// @param versionGroup 该机器适用的版本组引用
/// @author lishangbu
/// @since 2025/5/20
public record MachineVersionDetail(
        APIResource<?> machine, @JsonProperty("version_group") NamedApiResource<?> versionGroup) {}

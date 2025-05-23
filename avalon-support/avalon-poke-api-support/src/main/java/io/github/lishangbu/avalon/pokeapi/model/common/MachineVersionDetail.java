package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/MachineVersionDetail</a>
 *
 * @param machine 教导技能的机器来自物品
 * @param versionGroup 该机器的版本组
 * @author lishangbu
 * @since 2025/5/20
 */
public record MachineVersionDetail(
    APIResource<?> machine, @JsonProperty("version_group") NamedApiResource<?> versionGroup) {}

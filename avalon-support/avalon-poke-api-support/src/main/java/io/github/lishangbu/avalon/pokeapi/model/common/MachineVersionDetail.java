package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/MachineVersionDetail</a>
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record MachineVersionDetail(
    APIResource<?> machine, @JsonProperty("version_group") NamedApiResource<?> versionGroup) {

  /** 获取教导技能的机器来自物品 */
  public APIResource<?> machine() {
    return machine;
  }

  /** 获取该机器的版本组 */
  public NamedApiResource<?> versionGroup() {
    return versionGroup;
  }
}

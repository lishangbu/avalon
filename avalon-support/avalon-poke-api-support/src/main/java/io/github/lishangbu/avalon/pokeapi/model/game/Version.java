package io.github.lishangbu.avalon.pokeapi.model.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 游戏的版本，例如红、蓝或黄。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param names 不同语言中列出的此资源名称
 * @param versionGroup 此版本所属的版本组
 * @author lishangbu
 * @since 2025/5/24
 */
public record Version(
    Integer id,
    String name,
    List<Name> names,
    @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup) {}

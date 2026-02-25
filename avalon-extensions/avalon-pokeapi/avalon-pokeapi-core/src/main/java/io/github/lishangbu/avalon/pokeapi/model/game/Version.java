package io.github.lishangbu.avalon.pokeapi.model.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 游戏版本模型
///
/// 表示游戏版本（如 红/蓝/黄）及其多语言名称与所属版本组
///
/// @param id           资源标识符
/// @param name         资源名称
/// @param names        不同语言下的名称列表
/// @param versionGroup 此版本所属的版本组引用
/// @author lishangbu
/// @see Name
/// @see VersionGroup
/// @since 2025/5/24
public record Version(
        Integer id,
        String name,
        List<Name> names,
        @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup) {}

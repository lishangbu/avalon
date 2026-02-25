package io.github.lishangbu.avalon.pokeapi.model.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import java.util.List;

/// 招式学习方法模型
///
/// 表示宝可梦学习招式的方式
///
/// @param id            资源标识符
/// @param name          资源名称
/// @param descriptions  此资源在不同语言中的描述列表
/// @param names         此资源在不同语言下的名称列表
/// @param versionGroups 可以通过此方法学习招式的版本组列表
/// @author lishangbu
/// @see Description
/// @see Name
/// @see VersionGroup
/// @since 2025/6/7
public record MoveLearnMethod(
        Integer id,
        String name,
        List<Description> descriptions,
        List<Name> names,
        @JsonProperty("version_groups") List<NamedApiResource<VersionGroup>> versionGroups) {}

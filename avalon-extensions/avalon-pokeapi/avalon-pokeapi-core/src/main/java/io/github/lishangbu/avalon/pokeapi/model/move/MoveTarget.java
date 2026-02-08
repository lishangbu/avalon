package io.github.lishangbu.avalon.pokeapi.model.move;

import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 招式目标模型
///
/// 表示招式在战斗中可指向的目标类型，例如宝可梦、环境或其他招式
///
/// @param id           资源标识符
/// @param name         资源名称
/// @param descriptions 不同语言下的描述列表
/// @param moves        指向此目标的招式列表
/// @param names        不同语言下的名称列表
/// @author lishangbu
/// @see Description
/// @see Move
/// @see Name
/// @since 2025/6/7
public record MoveTarget(
    Integer id,
    String name,
    List<Description> descriptions,
    List<NamedApiResource<Move>> moves,
    List<Name> names) {}

package io.github.lishangbu.avalon.pokeapi.model.move;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 招式引起的异常状态模型
///
/// 表示战斗中招式导致的异常状态，详情参考 [Bulbapedia](https://bulbapedia.bulbagarden.net/wiki/Status_condition)
///
/// @param id    资源标识符
/// @param name  资源名称
/// @param moves 导致此状态异常的招式引用列表
/// @param names 不同语言下的名称列表
/// @author lishangbu
/// @see Move
/// @see Name
/// @since 2025/6/7
public record MoveAilment(
        Integer id, String name, List<NamedApiResource<Move>> moves, List<Name> names) {}

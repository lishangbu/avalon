package io.github.lishangbu.avalon.pokeapi.model.move;

import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 招式分类模型
///
/// 对招式效果的一种宽泛分类，用于将具相似效果的招式分组
///
/// @param id           资源标识符
/// @param name         资源名称
/// @param moves        属于此分类的招式列表
/// @param descriptions 多语言描述列表
/// @author lishangbu
/// @see Move
/// @see Description
/// @since 2025/6/7
public record MoveCategory(
        Integer id,
        String name,
        List<NamedApiResource<Move>> moves,
        List<Description> descriptions) {}

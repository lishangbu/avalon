package io.github.lishangbu.avalon.pokeapi.model.move;

import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 招式伤害类别模型
///
/// 表示招式的伤害类型，例如物理、特殊或非伤害性
///
/// @param id           资源标识符
/// @param name         资源名称
/// @param descriptions 多语言描述列表
/// @param moves        属于此伤害类别的招式列表
/// @param names        多语言名称列表
/// @author lishangbu
/// @see Description
/// @see Move
/// @see Name
/// @since 2025/6/7
public record MoveDamageClass(
    Integer id,
    String name,
    List<Description> descriptions,
    List<NamedApiResource<Move>> moves,
    List<Name> names) {}

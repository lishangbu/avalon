package io.github.lishangbu.avalon.pokeapi.model.move;

import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 非常宽泛的分类，松散地将招式效果分组。
 *
 * @param id 资源的标识符
 * @param name 资源的名称
 * @param moves 属于此分类的招式{@link Move}列表
 * @param descriptions 此资源在不同语言中列出的描述{@link Description}
 * @author lishangbu
 * @see Move
 * @see Description
 * @since 2025/6/7
 */
public record MoveCategory(
    Integer id, String name, List<NamedApiResource<Move>> moves, List<Description> descriptions) {}

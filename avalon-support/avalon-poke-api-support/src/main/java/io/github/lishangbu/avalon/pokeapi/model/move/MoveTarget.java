package io.github.lishangbu.avalon.pokeapi.model.move;

import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 战斗中招式可以指向的目标。目标可以是宝可梦、环境甚至其他招式。
 *
 * @param id 资源的标识符
 * @param name 资源的名称
 * @param descriptions 此资源在不同语言中列出的描述{@link Description}
 * @param moves 指向此目标的招式{@link Move}列表
 * @param names 此资源在不同语言中列出的名称{@link Name}
 * @author lishangbu
 * @see Description
 * @see Move
 * @see Name
 * @since 2025/6/7
 */
public record MoveTarget(
    Integer id,
    String name,
    List<Description> descriptions,
    List<NamedApiResource<Move>> moves,
    List<Name> names) {}

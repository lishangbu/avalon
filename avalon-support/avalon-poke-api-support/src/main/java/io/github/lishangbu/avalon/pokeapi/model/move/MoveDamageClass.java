package io.github.lishangbu.avalon.pokeapi.model.move;

import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 招式可以拥有的伤害类别，例如物理、特殊或非伤害性。
 *
 * @param id 资源的标识符
 * @param name 资源的名称
 * @param descriptions 此资源在不同语言中列出的描述{@link Description}
 * @param moves 属于此伤害类别的招式{@link Move}列表
 * @param names 此资源在不同语言中列出的名称{@link Name}
 * @author lishangbu
 * @see Description
 * @see Move
 * @see Name
 * @since 2025/6/7
 */
public record MoveDamageClass(
    Integer id,
    String name,
    List<Description> descriptions,
    List<NamedApiResource<Move>> moves,
    List<Name> names) {}

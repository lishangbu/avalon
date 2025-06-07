package io.github.lishangbu.avalon.pokeapi.model.move;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 招式导致的状态异常是战斗中使用招式造成的状态条件。参见 <a
 * href="https://bulbapedia.bulbagarden.net/wiki/Status_condition">Bulbapedia</a> 获取更多详情。
 *
 * @param id 资源的标识符
 * @param name 资源的名称
 * @param moves 导致此状态异常的招式{@link Move}列表
 * @param names 此资源在不同语言中列出的名称{@link Name}
 * @author lishangbu
 * @see Move
 * @see Name
 * @since 2025/6/7
 */
public record MoveAilment(
    Integer id, String name, List<NamedApiResource<Move>> moves, List<Name> names) {}

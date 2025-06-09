package io.github.lishangbu.avalon.pokeapi.model.move;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import java.util.List;

/**
 * 对战开拓区中使用的招式风格。更多详情参见 <a
 * href="http://bulbapedia.bulbagarden.net/wiki/Battle_Frontier_(Generation_III)">Bulbapedia</a>。
 *
 * @param id 资源的标识符
 * @param name 资源的名称
 * @param names 此资源在不同语言中列出的名称{@link Name}
 * @author lishangbu
 * @see Name
 * @since 2025/6/7
 */
public record MoveBattleStyle(Integer id, String name, List<Name> names) {}

package io.github.lishangbu.avalon.pokeapi.model.move;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import java.util.List;

/// 招式战斗风格模型
///
/// 对战开拓区（Battle Frontier）中使用的招式风格，详情参考
// [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Battle_Frontier_(Generation_III))

///
/// @param id    资源标识符
/// @param name  资源名称
/// @param names 不同语言下的名称列表
/// @author lishangbu
/// @see Name
/// @since 2025/6/7
public record MoveBattleStyle(Integer id, String name, List<Name> names) {}

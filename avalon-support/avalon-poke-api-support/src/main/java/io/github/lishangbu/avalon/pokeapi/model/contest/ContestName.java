package io.github.lishangbu.avalon.pokeapi.model.contest;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">Contests/Contest Types/ContestName (type)</a>
 *
 * @param name 竞赛的名称
 * @param color 与竞赛名称关联的颜色
 * @param language 名称所使用的语言
 * @author lishangbu
 * @since 2025/5/22
 */
public record ContestName(String name, String color, NamedApiResource<Language> language) {}

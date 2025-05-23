package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/FlavorText</a>
 *
 * @param flavorText API资源在特定语言中的本地化描述文本，注意该文本是从游戏文件中直接获取的，因此可能包含一些特殊字符
 *     这些字符可能需要替换为可见的可解码版本。更多信息请查看此<a
 *     href="https://github.com/veekun/pokedex/issues/218#issuecomment-339841781">问题</a>
 * @param language 该描述文本所使用的语言
 * @param version 该描述文本对应的游戏版本
 * @author lishangbu
 * @since 2025/5/20
 */
public record FlavorText(
    @JsonProperty("flavor_text") String flavorText,
    NamedApiResource<Language> language,
    NamedApiResource<?> version) {}

package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/// 宝可梦种类的“科学”名称
///
/// @param awesomeName 特定语言中 API 资源的本地化“科学”名称
/// @param language    此名称所使用的语言引用 {@link Language}
/// @author lishangbu
/// @since 2025/6/8
public record AwesomeName(
    @JsonProperty("awesome_name") String awesomeName, NamedApiResource<Language> language) {}

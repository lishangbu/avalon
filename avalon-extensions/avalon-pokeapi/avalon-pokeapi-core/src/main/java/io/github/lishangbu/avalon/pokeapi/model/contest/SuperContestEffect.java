package io.github.lishangbu.avalon.pokeapi.model.contest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.FlavorText;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 超级华丽大赛效果模型
///
/// @param id                该资源的唯一标识符
/// @param appeal            吸引力等级
/// @param flavorTextEntries 不同语言下的风味文本
/// @param moves             在超级华丽大赛中具有该效果的招式列表
/// @author lishangbu
/// @see FlavorText
/// @since 2025/5/23
public record SuperContestEffect(
        Integer id,
        Integer appeal,
        @JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries,
        List<NamedApiResource<?>> moves) {}

package io.github.lishangbu.avalon.pokeapi.model.contest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Effect;
import io.github.lishangbu.avalon.pokeapi.model.common.FlavorText;
import java.util.List;

/// 华丽大赛效果模型
///
/// @param id                资源唯一标识符
/// @param appeal            使用该招式获得的基础心数
/// @param jam               对手失去的基础心数
/// @param effectEntries     不同语言下的效果描述
/// @param flavorTextEntries 不同语言下的风味文本
/// @author lishangbu
/// @see Effect
/// @see FlavorText
/// @since 2025/5/23
public record ContestEffect(
    Integer id,
    Integer appeal,
    Integer jam,
    @JsonProperty("effect_entries") List<Effect> effectEntries,
    @JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries) {}

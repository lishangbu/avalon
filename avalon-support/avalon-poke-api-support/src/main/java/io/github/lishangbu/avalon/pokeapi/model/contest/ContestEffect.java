package io.github.lishangbu.avalon.pokeapi.model.contest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Effect;
import io.github.lishangbu.avalon.pokeapi.model.common.FlavorText;
import java.util.List;

/**
 * 华丽大赛效果指的是招式在华丽大赛中使用时产生的效果
 *
 * @param id 该资源的唯一标识符
 * @param appeal 使用该招式获得的基础心数
 * @param jam 对手失去的基础心数
 * @param effectEntries 不同语言下该华丽大赛效果{@link Effect}的描述
 * @param flavorTextEntries 不同语言下该华丽大赛效果的风味{@link FlavorText}文本
 * @author lishangbu
 * @see Effect
 * @see FlavorText
 * @since 2025/5/23
 */
public record ContestEffect(
    Integer id,
    Integer appeal,
    Integer jam,
    @JsonProperty("effect_entries") List<Effect> effectEntries,
    @JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries) {}

package io.github.lishangbu.avalon.pokeapi.model.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Effect;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 技能"投掷"与不同道具一起使用时的各种效果。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param effectEntries 使用不同语言列出的此投掷效果的结果{@link Effect}
 * @param items 具有此投掷效果的道具列表{@link Item}
 * @author lishangbu
 * @see Effect
 * @see Item
 * @since 2025/5/24
 */
public record ItemFlingEffect(
    Integer id,
    String name,
    @JsonProperty("effect_entries") List<Effect> effectEntries,
    List<NamedApiResource<Item>> items) {}

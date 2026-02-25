package io.github.lishangbu.avalon.pokeapi.model.encounter;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 遭遇条件值模型
///
/// @param id        该资源的唯一标识符
/// @param name      该资源的名称
/// @param condition 所属遭遇条件引用
/// @param names     该资源在不同语言下的名称
/// @author lishangbu
/// @see EncounterCondition
/// @see Name
/// @since 2025/5/23
public record EncounterConditionValue(
        Integer id,
        String name,
        NamedApiResource<EncounterCondition> condition,
        List<Name> names) {}

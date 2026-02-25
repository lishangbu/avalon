package io.github.lishangbu.avalon.pokeapi.model.encounter;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 遭遇条件模型
///
/// @param id     该资源的唯一标识符
/// @param name   该资源的名称
/// @param names  该资源在不同语言下的名称
/// @param values 该遭遇条件的所有可能取值列表
/// @author lishangbu
/// @see Name
/// @see EncounterConditionValue
/// @since 2025/5/23
public record EncounterCondition(
        Integer id,
        String name,
        List<Name> names,
        List<NamedApiResource<EncounterConditionValue>> values) {}

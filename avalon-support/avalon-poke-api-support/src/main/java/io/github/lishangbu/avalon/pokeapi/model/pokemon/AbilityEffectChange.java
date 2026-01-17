package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Effect;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import java.util.List;

/// 特性效果变更记录模型
///
/// 表示特性在不同版本组中的历史效果变更
///
/// @param effectEntries 先前的效果列表（多语言）
/// @param versionGroup  该效果变更所属的版本组引用
/// @author lishangbu
/// @see Effect
/// @see VersionGroup
/// @since 2025/6/8
public record AbilityEffectChange(
    @JsonProperty("effect_entries") List<Effect> effectEntries,
    @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup) {}

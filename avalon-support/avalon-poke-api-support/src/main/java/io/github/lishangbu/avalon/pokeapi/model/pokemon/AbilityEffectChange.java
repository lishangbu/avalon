package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Effect;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import java.util.List;

/**
 * 特性效果变更记录
 *
 * @param effectEntries 此特性在不同语言中列出的先前效果{@link Effect}
 * @param versionGroup 此特性的先前效果起源于哪个版本组{@link VersionGroup}
 * @author lishangbu
 * @see Effect
 * @see VersionGroup
 * @since 2025/6/8
 */
public record AbilityEffectChange(
    @JsonProperty("effect_entries") List<Effect> effectEntries,
    @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup) {}

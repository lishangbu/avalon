package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 性格对宝可梦状态的影响
///
/// @param maxChange 对引用的宝可梦状态的最大变化量
/// @param nature    引起变化的性格 {@link Nature}
/// @author lishangbu
/// @see Nature
/// @since 2025/6/8
public record NaturePokeathlonStatAffect(
    @JsonProperty("max_change") Integer maxChange, NamedApiResource<Nature> nature) {}

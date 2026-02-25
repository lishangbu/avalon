package io.github.lishangbu.avalon.pokeapi.model.move;

import com.fasterxml.jackson.annotation.JsonProperty;

/// 华丽大赛招式组合集合
///
/// 表示招式在普通与超级华丽大赛中可搭配使用的组合详情
///
/// @param normal     普通华丽大赛下的组合详情
/// @param superCombo 超级华丽大赛下的组合详情
/// @author lishangbu
/// @see ContestComboDetail
/// @since 2025/6/7
public record ContestComboSets(
        ContestComboDetail normal, @JsonProperty("super") ContestComboDetail superCombo) {}

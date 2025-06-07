package io.github.lishangbu.avalon.pokeapi.model.move;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 华丽大赛中招式组合集合的信息。
 *
 * @param normal 此招式可以在其前后使用的招式详情{@link ContestComboDetail}，在普通华丽大赛中可获得额外魅力点数
 * @param superCombo 此招式可以在其前后使用的招式详情{@link ContestComboDetail}，在超级华丽大赛中可获得额外魅力点数
 * @author lishangbu
 * @see ContestComboDetail
 * @since 2025/6/7
 */
public record ContestComboSets(
    ContestComboDetail normal, @JsonProperty("super") ContestComboDetail superCombo) {}

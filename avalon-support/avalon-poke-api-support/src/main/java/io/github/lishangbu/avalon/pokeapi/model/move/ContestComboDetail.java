package io.github.lishangbu.avalon.pokeapi.model.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 华丽大赛中招式组合的详细信息。
 *
 * @param useBefore 在此招式之前使用的招式{@link Move}列表
 * @param useAfter 在此招式之后使用的招式{@link Move}列表
 * @author lishangbu
 * @see Move
 * @since 2025/6/7
 */
public record ContestComboDetail(
    @JsonProperty("use_before") List<NamedApiResource<Move>> useBefore,
    @JsonProperty("use_after") List<NamedApiResource<Move>> useAfter) {}

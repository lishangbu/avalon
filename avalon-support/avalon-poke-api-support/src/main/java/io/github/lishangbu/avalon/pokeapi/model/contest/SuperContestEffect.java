package io.github.lishangbu.avalon.pokeapi.model.contest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.FlavorText;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 超级华丽大赛效果指的是招式在超级华丽大赛中使用时产生的效果
 *
 * @author lishangbu
 * @since 2025/5/23
 */
public record SuperContestEffect(
    Integer id,
    Integer appeal,
    @JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries,
    List<NamedApiResource<?>> moves) {
  /** 该资源的唯一标识符 */
  public Integer id() {
    return id;
  }

  /** 该超级华丽大赛效果的吸引力等级 */
  public Integer appeal() {
    return appeal;
  }

  /** 不同语言下该超级华丽大赛效果的风味文本 */
  public List<FlavorText> flavorTextEntries() {
    return flavorTextEntries;
  }

  /** 在超级华丽大赛中使用时具有该效果的招式列表 */
  public List<NamedApiResource<?>> moves() {
    return moves;
  }
}

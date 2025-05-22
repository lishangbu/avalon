package io.github.lishangbu.avalon.pokeapi.model.contest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.FlavorText;
import java.util.List;

/**
 * 华丽大赛效果指的是招式在华丽大赛中使用时产生的效果
 *
 * @author lishangbu
 * @since 2025/5/23
 */
public record ContestEffect(
    Integer id,
    Integer appeal,
    Integer jam,
    @JsonProperty("effect_entries") List<?> effectEntries,
    @JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries) {
  /** 该资源的唯一标识符 */
  public Integer id() {
    return id;
  }

  /** 使用该招式获得的基础心数 */
  public Integer appeal() {
    return appeal;
  }

  /** 对手失去的基础心数 */
  public Integer jam() {
    return jam;
  }

  /** 不同语言下该华丽大赛效果的描述 */
  public List<?> effectEntries() {
    return effectEntries;
  }

  /** 不同语言下该华丽大赛效果的风味文本 */
  public List<FlavorText> flavorTextEntries() {
    return flavorTextEntries;
  }
}

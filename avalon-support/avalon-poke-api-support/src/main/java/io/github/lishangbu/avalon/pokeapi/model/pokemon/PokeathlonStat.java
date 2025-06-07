package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import java.util.List;

/**
 * 宝可梦竞技状态是宝可梦在竞技场中表现的不同属性。在竞技场中，比赛在不同的赛道上进行；每个赛道对应不同的宝可梦竞技状态。 详见 <a
 * href="http://bulbapedia.bulbagarden.net/wiki/Pok%C3%A9athlon">Bulbapedia</a>。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param names 该资源在不同语言中列出的名称{@link Name}
 * @param affectingNatures 详细说明正面或负面影响此竞技状态的性格{@link NaturePokeathlonStatAffectSets}
 * @author lishangbu
 * @see Name
 * @see NaturePokeathlonStatAffectSets
 * @since 2025/6/8
 */
public record PokeathlonStat(
    Integer id,
    String name,
    List<Name> names,
    @JsonProperty("affecting_natures") NaturePokeathlonStatAffectSets affectingNatures) {}

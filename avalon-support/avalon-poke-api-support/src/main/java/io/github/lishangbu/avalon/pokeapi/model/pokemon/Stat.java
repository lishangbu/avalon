package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.APIResource;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveDamageClass;
import java.util.List;

/**
 * 属性决定了战斗的某些方面。每个宝可梦都有各种属性值，这些属性值会随着等级提升而增长，并且可以在战斗中被临时效果改变。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param gameIndex 游戏用于此属性的ID
 * @param isBattleOnly 此属性是否仅在战斗中存在
 * @param affectingMoves 详细说明正面或负面影响此属性的技能{@link MoveStatAffectSets}
 * @param affectingNatures 详细说明正面或负面影响此属性的性格{@link NatureStatAffectSets}
 * @param characteristics 当宝可梦的最高基础属性是此属性时，设置的特征{@link Characteristic}列表
 * @param moveDamageClass 与此属性直接相关的伤害类别{@link MoveDamageClass}
 * @param names 该资源在不同语言中列出的名称{@link Name}
 * @author lishangbu
 * @see MoveStatAffectSets
 * @see NatureStatAffectSets
 * @see Characteristic
 * @see MoveDamageClass
 * @see Name
 * @since 2025/6/8
 */
public record Stat(
    Integer id,
    String name,
    @JsonProperty("game_index") Integer gameIndex,
    @JsonProperty("is_battle_only") Boolean isBattleOnly,
    @JsonProperty("affecting_moves") MoveStatAffectSets affectingMoves,
    @JsonProperty("affecting_natures") NatureStatAffectSets affectingNatures,
    List<APIResource<Characteristic>> characteristics,
    @JsonProperty("move_damage_class") NamedApiResource<MoveDamageClass> moveDamageClass,
    List<Name> names) {}

package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.APIResource;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveDamageClass;
import java.util.List;

/// 属性模型
///
/// 属性决定了战斗的某些方面；可随等级增长并受战斗效果临时改变
///
/// @param id               资源标识符
/// @param name             资源名称
/// @param gameIndex        游戏侧用于此属性的 ID
/// @param isBattleOnly     此属性是否仅在战斗中存在
/// @param affectingMoves   影响此属性的技能集合 {@link MoveStatAffectSets}
/// @param affectingNatures 影响此属性的性格集合 {@link NatureStatAffectSets}
/// @param characteristics  当宝可梦最高基础属性为此属性时的特征列表 {@link Characteristic}
/// @param moveDamageClass  与此属性相关的伤害类别 {@link MoveDamageClass}
/// @param names            多语言名称列表 {@link Name}
/// @author lishangbu
/// @see MoveStatAffectSets
/// @see NatureStatAffectSets
/// @see Characteristic
/// @see MoveDamageClass
/// @see Name
/// @since 2025/6/8
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

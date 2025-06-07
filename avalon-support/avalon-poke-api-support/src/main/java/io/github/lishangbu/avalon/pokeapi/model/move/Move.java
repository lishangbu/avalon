package io.github.lishangbu.avalon.pokeapi.model.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.*;
import io.github.lishangbu.avalon.pokeapi.model.contest.ContestEffect;
import io.github.lishangbu.avalon.pokeapi.model.contest.ContestType;
import io.github.lishangbu.avalon.pokeapi.model.contest.SuperContestEffect;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;
import java.util.List;

/**
 * 招式是宝可梦在战斗中的技能。在战斗中，宝可梦每回合使用一个招式。一些招式（包括通过秘传机学习的招式）也可以在战斗外使用，通常用于清除障碍物或探索新区域。
 *
 * @param id 资源的标识符
 * @param name 资源的名称
 * @param accuracy 此招式成功的概率百分比值
 * @param effectChance 此招式效果发生的概率百分比值
 * @param pp 技能点数。此招式可以使用的次数
 * @param priority -8到8之间的值。设置战斗中招式执行的顺序。详见Bulbapedia
 * @param power 此招式的基础威力，如果没有基础威力则为0
 * @param contestCombos 需要此招式的普通和超级华丽大赛组合的详情
 * @param contestType 此招式在华丽大赛中给宝可梦带来的魅力类型
 * @param contestEffect 此招式在华丽大赛中的效果
 * @param damageClass 此招式对目标造成的伤害类型，例如物理
 * @param effectEntries 此招式在不同语言中列出的效果
 * @param effectChanges 此招式在游戏的不同版本组中曾经有过的效果列表
 * @param learnedByPokemon 可以学习此招式的宝可梦列表
 * @param flavorTextEntries 此招式在不同语言中列出的风味文本
 * @param generation 引入此招式的世代
 * @param machines 教授此招式的机器列表
 * @param meta 关于此招式的元数据
 * @param names 此资源在不同语言中列出的名称
 * @param pastValues 游戏不同版本组中招式资源值变化的列���
 * @param statChanges 此招式影响的属性列表及其影响程度
 * @param superContestEffect 此招式在超级华丽大赛中的效果
 * @param target 接收攻击效果的目标类型
 * @param type 此招式的元素类型
 * @author lishangbu
 * @since 2025/6/7
 */
public record Move(
    Integer id,
    String name,
    Integer accuracy,
    @JsonProperty("effect_chance") Integer effectChance,
    Integer pp,
    Integer priority,
    Integer power,
    @JsonProperty("contest_combos") ContestComboSets contestCombos,
    @JsonProperty("contest_type") NamedApiResource<ContestType> contestType,
    @JsonProperty("contest_effect") APIResource<ContestEffect> contestEffect,
    @JsonProperty("damage_class") NamedApiResource<MoveDamageClass> damageClass,
    @JsonProperty("effect_entries") List<VerboseEffect> effectEntries,
    @JsonProperty("effect_changes") List<?> effectChanges,
    @JsonProperty("learned_by_pokemon") List<NamedApiResource<?>> learnedByPokemon,
    @JsonProperty("flavor_text_entries") List<MoveFlavorText> flavorTextEntries,
    NamedApiResource<Generation> generation,
    List<MachineVersionDetail> machines,
    MoveMetaData meta,
    List<Name> names,
    @JsonProperty("past_values") List<PastMoveStatValues> pastValues,
    @JsonProperty("stat_changes") List<MoveStatChange> statChanges,
    @JsonProperty("super_contest_effect") APIResource<SuperContestEffect> superContestEffect,
    NamedApiResource<MoveTarget> target,
    NamedApiResource<Type> type) {}

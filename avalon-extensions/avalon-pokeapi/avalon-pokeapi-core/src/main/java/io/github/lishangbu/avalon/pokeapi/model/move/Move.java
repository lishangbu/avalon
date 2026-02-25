package io.github.lishangbu.avalon.pokeapi.model.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.*;
import io.github.lishangbu.avalon.pokeapi.model.contest.ContestEffect;
import io.github.lishangbu.avalon.pokeapi.model.contest.ContestType;
import io.github.lishangbu.avalon.pokeapi.model.contest.SuperContestEffect;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;
import java.util.List;

/// 招式模型
///
/// 表示宝可梦在战斗或非战斗中可使用的技能，包含命中率、威力、优先级及多语言描述等信息
///
/// @param id                 资源标识符
/// @param name               资源名称
/// @param accuracy           命中率（百分比）
/// @param effectChance       效果触发概率（百分比）
/// @param pp                 技能点数
/// @param priority           招式优先级（-8 到 8）
/// @param power              基础威力
/// @param contestCombos      华丽大赛招式组合详情
/// @param contestType        华丽大赛下的招式类型
/// @param contestEffect      华丽大赛效果引用
/// @param damageClass        伤害类别引用
/// @param effectEntries      多语言效果描述
/// @param effectChanges      不同版本组的效果变化
/// @param learnedByPokemon   可以学习此招式的宝可梦列表
/// @param flavorTextEntries  偏好文本（多语言）
/// @param generation         引入此招式的世代引用
/// @param machines           教授此招式的机器列表
/// @param meta               招式元数据
/// @param names              多语言名称
/// @param pastValues         历史版本的值变化
/// @param statChanges        招式影响的属性变化
/// @param superContestEffect 超级华丽大赛效果引用
/// @param target             招式目标类型引用
/// @param type               招式元素类型引用
/// @author lishangbu
/// @since 2025/6/7
public record Move(
        Integer id,
        String name,
        Integer accuracy,
        @JsonProperty("effect_chance") Integer effectChance,
        Integer pp,
        Integer priority,
        Integer power,
        @JsonProperty("contest_combos") ContestComboSets contestCombos,
        @JsonProperty("contestType") NamedApiResource<ContestType> contestType,
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

package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.*;
import io.github.lishangbu.avalon.pokeapi.model.evolution.EvolutionChain;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import java.util.List;

/// 宝可梦种类模型
///
/// 宝可梦种类是至少一种宝可梦的基础，其属性在该种类内的所有变种中共享。示例：Wormadam 存在三种变种
///
/// @param id                   资源标识符
/// @param name                 资源名称
/// @param order                排序顺序（全国图鉴顺序）
/// @param genderRate           雌性概率（以八分之一为单位），-1 表示无性别
/// @param captureRate          基础捕获率（最大 255）
/// @param baseHappiness        捕获时的初始亲密度（最大 255）
/// @param isBaby               是否为蛋孵化出的宝宝
/// @param isLegendary          是否为传说宝可梦
/// @param isMythical           是否为神话宝可梦
/// @param hatchCounter         初始孵化计数器
/// @param hasGenderDifferences 是否存在性别差异
/// @param formsSwitchable      是否存在可切换形态
/// @param growthRate           成长速率引用 {@link GrowthRate}
/// @param pokedexNumbers       图鉴条目列表 {@link PokemonSpeciesDexEntry}
/// @param eggGroups            所属蛋组列表 {@link EggGroup}
/// @param color                用于图鉴搜索的颜色 {@link PokemonColor}
/// @param shape                用于图鉴搜索的形状 {@link PokemonShape}
/// @param evolvesFromSpecies   进化来源 {@link PokemonSpecies}
/// @param evolutionChain       所属进化链 {@link EvolutionChain}
/// @param habitat              栖息地 {@link PokemonHabitat}
/// @param generation           引入该种类的世代 {@link Generation}
/// @param names                多语言名称列表 {@link Name}
/// @param palParkEncounters    伙伴公园遇到列表 {@link PalParkEncounterArea}
/// @param flavorTextEntries    风味文本条目 {@link FlavorText}
/// @param formDescriptions     形态描述列表 {@link Description}
/// @param genera               科属列表 {@link Genus}
/// @param varieties            可用变种列表 {@link PokemonSpeciesVariety}
/// @author lishangbu
/// @see GrowthRate
/// @see PokemonSpeciesDexEntry
/// @see EggGroup
/// @see PokemonColor
/// @see PokemonShape
/// @see PokemonSpecies
/// @see EvolutionChain
/// @see PokemonHabitat
/// @see Generation
/// @see Name
/// @see PalParkEncounterArea
/// @see FlavorText
/// @see Description
/// @see Genus
/// @see PokemonSpeciesVariety
/// @since 2025/6/8
public record PokemonSpecies(
        Integer id,
        String name,
        Integer order,
        @JsonProperty("gender_rate") Integer genderRate,
        @JsonProperty("capture_rate") Integer captureRate,
        @JsonProperty("base_happiness") Integer baseHappiness,
        @JsonProperty("is_baby") Boolean isBaby,
        @JsonProperty("is_legendary") Boolean isLegendary,
        @JsonProperty("is_mythical") Boolean isMythical,
        @JsonProperty("hatch_counter") Integer hatchCounter,
        @JsonProperty("has_gender_differences") Boolean hasGenderDifferences,
        @JsonProperty("forms_switchable") Boolean formsSwitchable,
        @JsonProperty("growth_rate") NamedApiResource<GrowthRate> growthRate,
        @JsonProperty("pokedex_numbers") List<PokemonSpeciesDexEntry> pokedexNumbers,
        @JsonProperty("egg_groups") List<NamedApiResource<EggGroup>> eggGroups,
        NamedApiResource<PokemonColor> color,
        NamedApiResource<PokemonShape> shape,
        @JsonProperty("evolves_from_species") NamedApiResource<PokemonSpecies> evolvesFromSpecies,
        @JsonProperty("evolution_chain") APIResource<EvolutionChain> evolutionChain,
        NamedApiResource<PokemonHabitat> habitat,
        NamedApiResource<Generation> generation,
        List<Name> names,
        @JsonProperty("pal_park_encounters") List<PalParkEncounterArea> palParkEncounters,
        @JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries,
        @JsonProperty("form_descriptions") List<Description> formDescriptions,
        List<Genus> genera,
        List<PokemonSpeciesVariety> varieties) {}

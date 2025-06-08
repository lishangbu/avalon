package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.*;
import io.github.lishangbu.avalon.pokeapi.model.evolution.EvolutionChain;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import java.util.List;

/**
 * 宝可梦种类是至少一种宝可梦的基础。宝可梦种类的属性在该种类内的所有宝可梦变种中共享。一个很好的例子是结草贵妇（Wormadam）；结草贵妇是一个可以在三种不同变种中找到的种类：垃圾结草贵妇、砂土结草贵妇和植物结草贵妇。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param order 宝可梦种类应该排序的顺序。基于全国图鉴顺序，但家族会被分组在一起并按阶段排序
 * @param genderRate 这个宝可梦为雌性的概率，以八分之几表示；或者-1表示无性别
 * @param captureRate 基础捕获率；最高255。数字越高，越容易捕获
 * @param baseHappiness 用普通精灵球捕获时的初始亲密度；最高255。数字越高，宝可梦越开心
 * @param isBaby 这是否为宝可梦宝宝
 * @param isLegendary 这是否为传说宝可梦
 * @param isMythical 这是否为神话宝可梦
 * @param hatchCounter
 *     初始孵化计数器：必须走Y×(hatch_counter+1)步才能孵化这个宝可梦的蛋，除非使用如火焰之躯等加成。Y因世代而异。在第二、第三和第七世代，蛋周期为256步。在第四世代，蛋周期为255步。在《宝可梦晶灿钻石与明亮珍珠》中，蛋周期也是255步，但在特殊日期会更短。在第五和第六世代，蛋周期为257步。在《宝可梦剑与盾》以及《宝可梦朱与紫》中，蛋周期为128步
 * @param hasGenderDifferences 这个宝可梦是否有可视的性别差异
 * @param formsSwitchable 这个宝可梦是否有多种形态并可以在它们之间切换
 * @param growthRate 这个宝可梦种类获得等级的速率{@link GrowthRate}
 * @param pokedexNumbers 为这个宝可梦种类保留的宝可梦图鉴及其索引{@link PokemonSpeciesDexEntry}列表
 * @param eggGroups 这个宝可梦种类所属的蛋组列表{@link EggGroup}
 * @param color 用于宝可梦图鉴搜索的宝可梦颜色{@link PokemonColor}
 * @param shape 用于宝可梦图鉴搜索的宝可梦形状{@link PokemonShape}
 * @param evolvesFromSpecies 进化成此宝可梦种类的宝可梦种类{@link PokemonSpecies}
 * @param evolutionChain 这个宝可梦种类所属的进化链{@link EvolutionChain}
 * @param habitat 可以遇到这个宝可梦种类的栖息地{@link PokemonHabitat}
 * @param generation 引入这个宝可梦种类的世代{@link Generation}
 * @param names 该资源在不同语言中列出的名称{@link Name}
 * @param palParkEncounters 在伙伴公园中可以与这个宝可梦种类相遇{@link PalParkEncounterArea}的列表
 * @param flavorTextEntries 这个宝可梦种类的风味文本条目{@link FlavorText}列表
 * @param formDescriptions 宝可梦在这个宝可梦种类内采取的不同形态的描述{@link Description}
 * @param genera 这个宝可梦种类在多种语言中列出的科属{@link Genus}
 * @param varieties 存在于这个宝可梦种类{@link PokemonSpeciesVariety}内的宝可梦列表
 * @author lishangbu
 * @see GrowthRate
 * @see PokemonSpeciesDexEntry
 * @see EggGroup
 * @see PokemonColor
 * @see PokemonShape
 * @see PokemonSpecies
 * @see EvolutionChain
 * @see PokemonHabitat
 * @see Generation
 * @see Name
 * @see PalParkEncounterArea
 * @see FlavorText
 * @see Description
 * @see Genus
 * @see PokemonSpeciesVariety
 * @since 2025/6/8
 */
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

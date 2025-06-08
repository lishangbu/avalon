package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.common.VersionGameIndex;
import java.util.List;

/**
 * 宝可梦是栖息在宝可梦游戏世界中的生物。它们可以通过精灵球捕获，并通过与其他宝可梦对战进行训练。每个宝可梦属于特定的物种，但可能呈现出与同种宝可梦不同的变种，如基础属性值、可用特性和属性类型等。详见
 * <a href="http://bulbapedia.bulbagarden.net/wiki/Pok%C3%A9mon_(species)">Bulbapedia</a>。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param baseExperience 击败此宝可梦获得的基础经验值
 * @param height 此宝可梦的身高（以分米为单位）
 * @param isDefault 用于标记每个物种的默认宝可梦，每个物种只有一个宝可梦设置为默认
 * @param order 用于排序的顺序。几乎是全国图鉴顺序，除了家族会被分组在一起
 * @param weight 此宝可梦的体重（以百克为单位）
 * @param abilities 此宝可梦可能拥有的特性{@link PokemonAbility}列表
 * @param forms 此宝可梦可以采取的形态{@link PokemonForm}列表
 * @param gameIndices 与各世代宝可梦相关的游戏索引{@link VersionGameIndex}列表
 * @param heldItems 遇到此宝可梦时可能持有的道具{@link PokemonHeldItem}列表
 * @param locationAreaEncounters 一个链接，指向包含特定版本的地点区域和遭遇详情的列表
 * @param moves 招式{@link PokemonMove}列表，包括特定版本组的学习方法和等级详情
 * @param pastTypes 显示此宝可梦在以前世代中的属性{@link PokemonTypePast}的详细列表
 * @param pastAbilities 显示此宝可梦在以前世代中的特性{@link PokemonAbilityPast}的详细列表
 * @param sprites 用于在游戏中描绘此宝可梦的精灵图像{@link PokemonSprites}集合。各种精灵图像的可视化表示可在PokeAPI/sprites找到
 * @param cries 用于在游戏中表现此宝可梦的叫声{@link PokemonCries}集合。各种叫声的可视化表示可在PokeAPI/cries找到
 * @param species 此宝可梦所属的物种{@link PokemonSpecies}
 * @param stats 此宝可梦的基础属性值{@link PokemonStat}列表
 * @param types 显示此宝可梦拥有的属性类型{@link PokemonType}的详细列表
 * @author lishangbu
 * @see PokemonAbility
 * @see PokemonForm
 * @see VersionGameIndex
 * @see PokemonHeldItem
 * @see PokemonMove
 * @see PokemonTypePast
 * @see PokemonAbilityPast
 * @see PokemonSprites
 * @see PokemonCries
 * @see PokemonSpecies
 * @see PokemonStat
 * @see PokemonType
 * @since 2025/6/8
 */
public record Pokemon(
    Integer id,
    String name,
    @JsonProperty("base_experience") Integer baseExperience,
    Integer height,
    @JsonProperty("is_default") Boolean isDefault,
    Integer order,
    Integer weight,
    List<PokemonAbility> abilities,
    List<NamedApiResource<PokemonForm>> forms,
    @JsonProperty("game_indices") List<VersionGameIndex> gameIndices,
    @JsonProperty("held_items") List<PokemonHeldItem> heldItems,
    @JsonProperty("location_area_encounters") String locationAreaEncounters,
    List<PokemonMove> moves,
    @JsonProperty("past_types") List<PokemonTypePast> pastTypes,
    @JsonProperty("past_abilities") List<PokemonAbilityPast> pastAbilities,
    PokemonSprites sprites,
    PokemonCries cries,
    NamedApiResource<PokemonSpecies> species,
    List<PokemonStat> stats,
    List<PokemonType> types) {}

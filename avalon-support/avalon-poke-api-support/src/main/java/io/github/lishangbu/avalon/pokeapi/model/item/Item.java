package io.github.lishangbu.avalon.pokeapi.model.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.*;
import java.util.List;

/**
 * 道具是一种能够被收集和使用的对象，例如在宝可梦的世界中可以使用药剂、球，或者教授给宝可梦技能的技能机器等。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param cost 此道具在商店中的价格
 * @param flingPower 使用此道具进行"投掷"行动时的威力
 * @param flingEffect 使用此道具进行"投掷"{@link ItemFlingEffect}行动时的效果
 * @param attributes 此道具具有的属性{@link ItemAttribute}列表
 * @param category 此道具所属的类别{@link ItemCategory}
 * @param effectEntries 不同语言中列出的此道具的效果{@link VerboseEffect}
 * @param flavorTextEntries 不同语言中列出的此道具的风味文本{@link VersionGroupFlavorText}
 * @param gameIndices 按世代列出的与此道具相关的游戏索引列表{@link GenerationGameIndex}
 * @param names 不同语言中列出的此道具的名称{@link Name}
 * @param sprites 用于在游戏中描绘此道具的精灵图集{@link ItemSprites}
 * @param heldByPokemon 可能在野外持有此道具的宝可梦列表{@link ItemHolderPokemon}
 * @param babyTriggerFor 此道具在繁殖过程中产生婴儿所需的进化链
 * @param machines 与此道具相关的机器列表
 * @see ItemFlingEffect
 * @see ItemAttribute
 * @see ItemCategory
 * @see VerboseEffect
 * @see VersionGroupFlavorText
 * @see GenerationGameIndex
 * @see Name
 * @see ItemSprites
 * @see ItemHolderPokemon
 * @author lishangbu
 * @since 2025/5/24
 */
public record Item(
    Integer id,
    String name,
    Integer cost,
    @JsonProperty("fling_power") Integer flingPower,
    @JsonProperty("fling_effect") NamedApiResource flingEffect,
    List<NamedApiResource> attributes,
    NamedApiResource<ItemCategory> category,
    @JsonProperty("effect_entries") List<VerboseEffect> effectEntries,
    @JsonProperty("flavor_text_entries") List<VersionGroupFlavorText> flavorTextEntries,
    @JsonProperty("game_indices") List<GenerationGameIndex> gameIndices,
    List<Name> names,
    ItemSprites sprites,
    @JsonProperty("held_by_pokemon") List<ItemHolderPokemon> heldByPokemon,
    @JsonProperty("baby_trigger_for") APIResource<?> babyTriggerFor,
    List<?> machines) {}

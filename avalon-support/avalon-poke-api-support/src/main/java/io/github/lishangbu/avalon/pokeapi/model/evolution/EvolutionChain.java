package io.github.lishangbu.avalon.pokeapi.model.evolution;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;

/**
 * 进化链是从基本形态到最终进化形态的完整宝可梦进化路径。 链接强调了每个宝可梦的进化进程中的复杂关系。
 *
 * @param id 该资源的标识符
 * @param babyTriggerItem 宝可梦在繁殖时持有的物品{@link Item}，会触发孵化出幼年宝可梦而不是基本宝可梦
 * @param chain 基本链接对象。每个链接包含该链中宝可梦的进化详细信息，每个链接引用自然进化顺序中的下一个宝可梦
 * @author lishangbu
 * @see Item
 * @since 2025/5/24
 */
public record EvolutionChain(
    Integer id,
    @JsonProperty("baby_trigger_item") NamedApiResource<Item> babyTriggerItem,
    ChainLink chain) {}

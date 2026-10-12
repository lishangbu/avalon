package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.creature.model.EvolutionDropDefinition;
import io.github.lishangbu.avalon.content.creature.model.EvolutionDropDrop;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 普通野生幻兽的进化材料双池；精英池仍由野外模板单独决定。 */
@Component
public class EvolutionDropCatalog {

	private final Map<String, EvolutionDropDrop> species;

	public EvolutionDropCatalog(ItemCatalog items, CreatureCatalog creatures) {
		var definition = ContentLoader.read("/content/evolution-drops.json", EvolutionDropDefinition.class);
		definition.species().forEach((id, drop) -> {
			if (!creatures.obtainableDirectly(id) || !items.hasItem(drop.common()) || !items.hasItem(drop.family())
					|| items.item(drop.common()).task() || items.item(drop.family()).task()
					|| drop.common().equals(drop.family()))
				throw new IllegalArgumentException("野生进化材料配置无效：" + id);
		});
		species = Map.copyOf(definition.species());
	}

	public EvolutionDropDrop drop(String id) {
		return species.get(id);
	}

}

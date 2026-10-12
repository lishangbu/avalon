package io.github.lishangbu.avalon.inventory.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.item.EquipmentCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.inventory.application.model.EquipmentShopProduct;
import io.github.lishangbu.avalon.inventory.application.model.EquipmentShopShop;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 商店展示与购买命令共用资格投影，当前位置、战斗状态和已完成任务均来自服务端。 */
@Component
@RequiredArgsConstructor
public class EquipmentShopQueryService {

	private final CharacterQueryService characters;

	private final CharacterFactQueryService facts;

	private final EquipmentCatalog catalog;

	private final ItemCatalog items;

	@Transactional
	public EquipmentShopShop view(long owner) {
		var character = characters.find(owner);
		if (character == null || character.activeBattleId() != null
				|| !catalog.definition().shops().contains(character.location()))
			return new EquipmentShopShop(false, List.of());
		var completed = facts.codes(owner);
		var products = catalog.definition()
			.offers()
			.stream()
			.filter(offer -> completed.contains("QUEST:" + offer.requires())
					&& offer.locations().contains(character.location()))
			.map(offer -> {
				var gear = items.equipment(offer.code());
				var metadata = catalog.metadata(offer.code());
				Map<String, Integer> stats = new TreeMap<>(gear.stats());
				gear.professionStats()
					.getOrDefault(character.profession(), Map.of())
					.forEach((key, value) -> stats.merge(key, value, Integer::sum));
				return new EquipmentShopProduct(offer.code(),
						gear.names().getOrDefault(character.profession(), gear.name()), gear.slot(), gear.level(),
						metadata.quality(), metadata.price(), Map.copyOf(stats));
			})
			.toList();
		return new EquipmentShopShop(true, products);
	}

}

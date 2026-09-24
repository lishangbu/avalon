package io.github.lishangbu.avalon.inventory.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.item.BookRecyclingCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 同编号跨来源统计可用库存；绑定书可保留，待领数量与已学技能不参与重复判定。 */
@Component
@RequiredArgsConstructor
public class BookRecyclingQueryService {

	private final CharacterQueryService characters;

	private final InventoryRepository inventory;

	private final BookRecyclingCatalog catalog;

	private final ItemCatalog items;

	@Transactional
	public View view(long owner) {
		var character = characters.find(owner);
		if (character == null || character.activeBattleId() != null || !catalog.available(character.location()))
			return new View(false, List.of());
		var stock = inventory.available(Long.parseLong(character.id()));
		var offers = catalog.prices()
			.entrySet()
			.stream()
			.sorted(Map.Entry.comparingByKey())
			.filter(entry -> !items.item(entry.getKey()).task())
			.map(entry -> {
				long total = stock.stream()
					.filter(i -> i.getCode().equals(entry.getKey()))
					.mapToLong(i -> i.getQuantity())
					.sum();
				long unbound = stock.stream()
					.filter(i -> i.getCode().equals(entry.getKey()) && !i.isBound())
					.mapToLong(i -> i.getQuantity())
					.sum();
				long maximum = Math.min(unbound, Math.max(0, total - 1));
				return new Offer(entry.getKey(), items.item(entry.getKey()).name(), entry.getValue(), total, unbound,
						maximum);
			})
			.filter(offer -> offer.maximum() > 0)
			.toList();
		return new View(true, offers);
	}

	public record View(boolean available, List<Offer> offers) {
	}

	public record Offer(String code, String name, int unitPrice, long total, long unbound, long maximum) {
	}

}

package io.github.lishangbu.avalon.battle.application.query;

import io.github.lishangbu.avalon.battle.application.model.PartyFactory;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.content.life.FoodCatalog;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FoodQueryService {

	private final PartyFactory party;

	private final CombatVitalsRepository vitals;

	private final InventoryQueryService inventory;

	private final FoodCatalog foods;

	@Transactional
	public View view(long owner) {
		var targets = party.create(owner, true)
			.stream()
			.filter(unit -> unit.isHero() || unit.getPosition() < 100)
			.map(unit -> {
				var stored = vitals.get(Long.parseLong(unit.getId()));
				return new Target(unit.getId(), unit.getName(),
						stored == null ? unit.getHp() : Math.min(stored.getHp(), unit.getHp()),
						stored == null ? unit.getMp() : Math.min(stored.getMp(), unit.getMp()), unit.getHp(),
						unit.getMp());
			})
			.toList();
		return new View(inventory.list(owner).stream().filter(item -> foods.food(item.code()) != null).toList(),
				foods.foods(), targets);
	}

	public record Target(String id, String name, int health, int mana, int maximumHealth, int maximumMana) {
	}

	public record View(List<InventoryItemView> inventory, List<FoodCatalog.Food> foods, List<Target> targets) {
	}

}

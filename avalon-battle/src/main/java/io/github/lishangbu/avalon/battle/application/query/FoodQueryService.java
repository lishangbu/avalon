package io.github.lishangbu.avalon.battle.application.query;

import io.github.lishangbu.avalon.battle.application.model.FoodTarget;
import io.github.lishangbu.avalon.battle.application.model.FoodView;
import io.github.lishangbu.avalon.battle.application.model.PartyFactory;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.content.life.FoodCatalog;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import jakarta.transaction.Transactional;
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
	public FoodView view(long owner) {
		var targets = party.create(owner, true)
			.stream()
			.filter(unit -> unit.isHero() || unit.getPosition() < 100)
			.map(unit -> {
				var stored = vitals.get(Long.parseLong(unit.getId()));
				return new FoodTarget(unit.getId(), unit.getName(),
						stored == null ? unit.getHp() : Math.min(stored.getHp(), unit.getHp()),
						stored == null ? unit.getMp() : Math.min(stored.getMp(), unit.getMp()), unit.getHp(),
						unit.getMp());
			})
			.toList();
		return new FoodView(inventory.list(owner).stream().filter(item -> foods.food(item.code()) != null).toList(),
				foods.foods(), targets);
	}

}

package io.github.lishangbu.avalon.inventory.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.inventory.persistence.repository.DungeonSupplyScopeRepository;
import io.github.lishangbu.avalon.inventory.persistence.repository.DungeonSupplyAllocationRepository;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.TreeMap;

/** 只读本次物资数量，不隐式登记、领取或回填额度。客户端数量不能用作扣款依据。 */
@Component
@RequiredArgsConstructor
public class DungeonSupplyQueryService {

	private final CharacterQueryService characters;

	private final DungeonSupplyScopeRepository scopes;

	private final DungeonSupplyAllocationRepository allocations;

	private final InventoryRepository inventory;

	@Transactional
	public Map<String, Integer> available(long run, long owner) {
		long character = characters.requireId(owner);
		var scope = scopes.scope(run, character);
		if (scope == null || !scope.isActive())
			return Map.of();
		Map<String, Integer> result = new TreeMap<>();
		for (var entry : allocations.allocations(scope.getId())) {
			var item = inventory.get(entry.getItemId());
			if (item.getCharacterId() != character)
				throw new IllegalStateException("副本物资归属损坏");
			int quantity = Math.min(entry.getRemaining(), item.getQuantity());
			if (quantity > 0)
				result.merge(item.getCode(), quantity, Math::addExact);
		}
		return Map.copyOf(result);
	}

}

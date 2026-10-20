package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.inventory.persistence.entity.DungeonSupply;
import io.github.lishangbu.avalon.inventory.persistence.entity.DungeonSupplyScope;
import io.github.lishangbu.avalon.inventory.persistence.entity.OwnedItem;
import io.github.lishangbu.avalon.inventory.persistence.repository.DungeonSupplyScopeRepository;
import io.github.lishangbu.avalon.inventory.persistence.repository.DungeonSupplyAllocationRepository;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内部副本物资命令，必须参与开本、回合或奖励事务，不提供 HTTP 任意登记入口。 调用方先锁进度／战局，再按固定顺序锁成员；本服务继续持角色锁，不反向锁副本。
 */
@Component
@RequiredArgsConstructor
@Transactional(Transactional.TxType.MANDATORY)
public class DungeonSupplyCommandService {

	private final CharacterCommandService characters;

	private final InventoryRepository inventory;

	private final InventoryCommandService grants;

	private final DungeonSupplyScopeRepository scopes;

	private final DungeonSupplyAllocationRepository allocations;

	private final DungeonCatalog catalog;

	/** 入场只登记当时已经在背包里的实例数量；空包也登记，重试不能吸收后来的外部物品。 */
	public void enter(long run, long owner) {
		enter(run, owner, catalog.definition().id());
	}

	/** 调用方从持锁进度读取副本编号，不接受客户端自由指定补给白名单。 */
	public void enter(long run, long owner, String dungeon) {
		var definition = catalog.definition(dungeon);
		if (run <= 0)
			throw new IllegalArgumentException("副本进度编号必须为正");
		long character = characters.lock(owner).id();
		var previous = scopes.scope(run, character);
		if (previous != null) {
			if (!previous.isActive())
				throw new BusinessException(409, "已退出此副本物资登记");
			return;
		}
		if (scopes.hasActive(character))
			throw new BusinessException(409, "已经登记了其他副本物资");
		var scope = new DungeonSupplyScope();

		scope.setRunId(run);
		scope.setCharacterId(character);
		scope.setActive(true);
		scopes.save(scope);
		inventory.available(character)
			.stream()
			.filter(item -> definition.supplies().contains(item.getCode()))
			.forEach(item -> allocate(scope, item, item.getQuantity()));
	}

	/**
	 * 本次副本新产出的药品可登记全部奖励额度；满包部分仍须实际领取才能用。 来源由副本编号及内部奖励键构成，不接受客户端指定任意已有物品扩大额度。
	 */
	public void reward(long run, long owner, String rewardKey, String code, int amount) {
		reward(run, owner, catalog.definition().id(), rewardKey, code, amount);
	}

	public void reward(long run, long owner, String dungeon, String rewardKey, String code, int amount) {
		var definition = catalog.definition(dungeon);
		long character = characters.lock(owner).id();
		var scope = require(run, character);
		if (rewardKey == null || rewardKey.isBlank() || amount <= 0 || !definition.supplies().contains(code))
			throw new IllegalArgumentException("副本补给奖励参数无效");
		String source = "DUNGEON:" + run + ":" + rewardKey;
		grants.grant(owner, source, code, amount, false);
		var item = inventory.source(character, source);
		if (!item.getCode().equals(code))
			throw new IllegalStateException("同一副本奖励来源不能改变物品");
		if (allocations.allocations(scope.getId()).stream().noneMatch(entry -> entry.getItemId().equals(item.getId())))
			allocate(scope, item, Math.addExact(item.getQuantity(), item.getPendingQuantity()));
	}

	/** 额度和真实库存一起扣；不调用通用同名扣款，避免误吃外部来源实例。 */
	public void take(long run, long owner, String code, int amount) {
		if (amount <= 0)
			throw new IllegalArgumentException("副本消耗数量必须为正");
		long character = characters.lock(owner).id();
		var scope = require(run, character);
		Map<DungeonSupply, OwnedItem> matching = new LinkedHashMap<>();
		for (var entry : allocations.allocations(scope.getId())) {
			var item = inventory.get(entry.getItemId());
			if (item.getCharacterId() != character)
				throw new IllegalStateException("副本物资归属损坏");
			if (item.getCode().equals(code))
				matching.put(entry, item);
		}
		long available = matching.entrySet()
			.stream()
			.mapToLong(entry -> Math.min(entry.getKey().getRemaining(), entry.getValue().getQuantity()))
			.sum();
		if (available < amount)
			throw new BusinessException(409, "本次副本可用补给不足");
		for (var entry : matching.entrySet()) {
			int used = Math.min(amount, Math.min(entry.getKey().getRemaining(), entry.getValue().getQuantity()));
			entry.getKey().setRemaining(entry.getKey().getRemaining() - used);
			entry.getValue().setQuantity(entry.getValue().getQuantity() - used);
			amount -= used;
			if (amount == 0)
				break;
		}
	}

	/** 退出或通关关闭登记，不删除来源、不返还已用药品；余下物品仍留在真实背包。 */
	public void close(long run, long owner) {
		long character = characters.lock(owner).id();
		var scope = scopes.scope(run, character);
		if (scope == null)
			throw new BusinessException(404, "副本物资登记不存在");
		scope.setActive(false);
	}

	private DungeonSupplyScope require(long run, long character) {
		var scope = scopes.scope(run, character);
		if (scope == null || !scope.isActive())
			throw new BusinessException(409, "不在此副本物资登记中");
		return scope;
	}

	private void allocate(DungeonSupplyScope scope, OwnedItem item, int quantity) {
		var entry = new DungeonSupply();

		entry.setScopeId(scope.getId());
		entry.setItemId(item.getId());
		entry.setRemaining(quantity);
		allocations.save(entry);
	}

}

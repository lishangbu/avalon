package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;
import io.github.lishangbu.avalon.inventory.persistence.entity.OwnedItem;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryReconciliationRepository;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.Comparator;

/** 一次性整理升级前的超容量库存；继续使用正式容量规则，不丢弃历史奖励。 */
@Component
@RequiredArgsConstructor
public class InventoryReconciliationService {

	private final CharacterCommandService characters;

	private final InventoryRepository items;

	private final InventoryReconciliationRepository queue;

	private final ItemCatalog catalog;

	@Transactional
	public void repair(long owner) {
		long character = characters.lock(owner).id();
		if (!queue.pending(character))
			return;
		var accepted = new ArrayList<BagCapacityRules.Entry>();
		// 已穿戴装备优先占位，防止上线整理改变属性或在进行中的战局里卸装。
		var ordered = items.available(character)
			.stream()
			.sorted(Comparator.comparing(OwnedItem::isEquipped).reversed().thenComparing(OwnedItem::getId))
			.toList();
		for (var item : ordered) {
			boolean gear = item.getSlot() != null;
			boolean task = !gear && catalog.item(item.getCode()).task();
			var entry = new BagCapacityRules.Entry(item.getCode(), item.isBound(), item.getQuantity(), gear, task);
			int fits = BagCapacityRules.fits(accepted, entry);
			if (item.isEquipped() && fits != item.getQuantity())
				throw new IllegalStateException("历史穿戴库存异常，拒绝自动卸装");
			item.setPendingQuantity(Math.addExact(item.getPendingQuantity(), item.getQuantity() - fits));
			item.setQuantity(fits);
			if (fits > 0)
				accepted.add(new BagCapacityRules.Entry(item.getCode(), item.isBound(), fits, gear, task));
		}
		// 队列删除与库存转移同事务，宕机或任意持久化失败都可在下次启动重试。
		queue.complete(character);
	}

}

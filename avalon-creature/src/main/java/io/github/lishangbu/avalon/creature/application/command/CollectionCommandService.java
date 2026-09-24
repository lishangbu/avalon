package io.github.lishangbu.avalon.creature.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.creature.CollectionCatalog;
import io.github.lishangbu.avalon.creature.persistence.entity.CollectionReceipt;
import io.github.lishangbu.avalon.creature.persistence.repository.CollectionReceiptRepository;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureDiscoveryRecords;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureDiscoveryRepository;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 图鉴奖励随真实获得一起结算，放生历史仍参与首次发现判断。 主动补领支持旧存档；读取图鉴不会修改金币。凭证、金币、物品及捕获共用外层事务。
 */
@Component
@RequiredArgsConstructor
public class CollectionCommandService {

	private final CharacterCommandService characters;

	private final CreatureRepository pets;

	private final CreatureDiscoveryRecords discoveryRecords;

	private final CreatureDiscoveryRepository discoveries;

	private final CollectionReceiptRepository receipts;

	private final CollectionCatalog catalog;

	private final InventoryCommandService inventory;

	@Transactional
	public void claim(long owner) {
		characters.requireOutsideBattle(owner);
		settle(owner);
	}

	/** 捕获战斗内也需要结算，故内部入口只取锁，不使用战外门禁。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void settle(long owner) {
		long character = characters.lock(owner).id();
		// 补领可吸收历史导入的个体；查询保持只读，正式获得入口与此处共同提交。
		pets.history(character).forEach(discoveries::remember);
		var history = discoveryRecords.discovered(character);
		var species = history.stream().map(p -> p.getSpecies()).collect(Collectors.toCollection(TreeSet::new));
		var credited = receipts.owned(owner)
			.stream()
			.map(CollectionReceipt::getCode)
			.collect(Collectors.toCollection(HashSet::new));
		for (String code : species)
			if (credited.add("SPECIES:" + code))
				award(owner, "SPECIES:" + code, catalog.reward(code));
		long milestoneCount = species.stream().filter(catalog.definition().milestoneSpecies()::contains).count();
		for (var milestone : catalog.definition().milestones()) {
			String code = "COUNT:" + milestone.count();
			if (milestoneCount >= milestone.count() && credited.add(code))
				award(owner, code, milestone.reward());
		}
		// 隐藏发现只有图鉴与称号进度，不发金币、道具或战斗属性。
		for (var pet : history) {
			String code = "HIDDEN:" + pet.getSpecies();
			if (pet.getAbilitySlot().equals("H") && credited.add(code))
				award(owner, code, null);
		}
	}

	private void award(long owner, String code, CollectionCatalog.Reward reward) {
		var receipt = new CollectionReceipt();

		receipt.setAccountId(owner);
		receipt.setCode(code);
		receipt.setCreatedAt(Instant.now());
		if (reward != null) {
			characters.changeGold(owner, reward.gold());
			inventory.grant(owner, "COLLECTION:" + code, reward.item(), reward.quantity(), false);
			receipt.setGold(reward.gold());
			receipt.setItemCode(reward.item());
			receipt.setQuantity(reward.quantity());
		}
		receipts.saveAndFlush(receipt);
	}

}

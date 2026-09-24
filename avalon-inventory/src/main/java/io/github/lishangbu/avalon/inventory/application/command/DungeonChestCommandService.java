package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.battle.DungeonChestCatalog;
import io.github.lishangbu.avalon.inventory.domain.DungeonChestRules;
import io.github.lishangbu.avalon.inventory.persistence.entity.DungeonChest;
import io.github.lishangbu.avalon.inventory.persistence.mapper.DungeonChestCodec;
import io.github.lishangbu.avalon.inventory.persistence.repository.DungeonChestRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.SplittableRandom;

/** 宝箱生成加入首领结算事务；领取、入包或转待领、金币与防重标记共同提交。 */
@Component
@RequiredArgsConstructor
public class DungeonChestCommandService {

	private final CharacterCommandService characters;

	private final DungeonChestRepository chests;

	private final DungeonChestRules rules;

	private final DungeonChestCatalog catalog;

	private final InventoryCommandService inventory;

	private final DungeonChestCodec codec;

	/** 仅由已核验发起者资格的内部胜利结算调用，不提供任意生成宝箱的 HTTP 接口。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void award(long owner, long run, long seed) {
		long character = characters.lock(owner).id();
		var existing = chests.byRun(run);
		if (existing != null) {
			if (existing.getCharacterId() != character)
				throw new IllegalStateException("副本宝箱发起者发生变化");
			return;
		}
		var chest = new DungeonChest();

		chest.setCharacterId(character);
		chest.setRunId(run);
		chest.setName(catalog.definition().name());
		chest.setCreatedAt(Instant.now());
		// 独立随机流不消耗战斗随机序列；事务重试仍给同一场胜利相同抽取结果。
		chest.setLoot(codec.encode(rules.roll(new SplittableRandom(seed ^ run ^ character))));
		chests.save(chest);
	}

	/** 以宝箱编号作为天然幂等键。已领取重试成功返回，不再发放任何资源。 */
	@Transactional
	public void claim(long owner, long id) {
		long character = characters.lock(owner).id();
		var chest = chests.get(id);
		if (chest == null || chest.getCharacterId() != character)
			throw new BusinessException(404, "宝箱不存在");
		if (chest.isClaimed())
			return;
		characters.requireOutsideDungeon(owner);
		var loot = codec.decode(chest.getLoot());
		String source = "DUNGEON_CHEST:" + id;
		inventory.grantRewardEquipment(owner, source + ":GEAR", loot.equipment(), loot.sellPrice());
		for (var extra : loot.extras())
			inventory.grantWithBinding(owner, source + ":" + extra.code(), extra.code(), extra.amount(), false,
					extra.bound());
		if (loot.gold() > 0)
			characters.changeGold(owner, loot.gold());
		chest.setClaimed(true);
	}

}

package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.item.EnhancementCatalog;
import io.github.lishangbu.avalon.inventory.application.model.CultivationMaterialMode;
import io.github.lishangbu.avalon.inventory.application.model.EnhancementAccess;
import io.github.lishangbu.avalon.inventory.application.model.EnhancementPreview;
import io.github.lishangbu.avalon.inventory.domain.EnhancementRules;
import io.github.lishangbu.avalon.inventory.persistence.mapper.InventoryStorageCodec;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 短暂持角色锁生成一致报价；预览不扣费、不消耗材料、不抽取随机结果。 */
@Component
@RequiredArgsConstructor
public class EnhancementPreviewService {

	private final CharacterCommandService characters;

	private final EnhancementCatalog catalog;

	private final InventoryRepository inventory;

	private final InventoryStorageCodec codec;

	private final CultivationMaterialService materials;

	private final CharacterQueryService characterQueries;

	/** 页面资格不依赖是否已有材料，未开放人物不会显示可以办理的入口。 */
	@Transactional
	public EnhancementAccess access(long owner) {
		var character = characterQueries.find(owner);
		if (character == null)
			return new EnhancementAccess(false, catalog.definition().playerLevel());
		var adventure = characterQueries.adventure(owner);
		return new EnhancementAccess(character.level() >= catalog.definition().playerLevel()
				&& catalog.definition().locations().contains(character.location()) && adventure.battle() == null
				&& adventure.dungeon() == null, catalog.definition().playerLevel());
	}

	@Transactional
	public EnhancementPreview preview(long owner, long id, CultivationMaterialMode requestedMode) {
		var context = characters.requireOutsideDungeon(owner);
		if (context.level() < catalog.definition().playerLevel())
			throw new BusinessException(409, "人物等级尚未达到强化要求");
		if (!catalog.definition().locations().contains(context.location()))
			throw new BusinessException(409, "请到城镇强化设施办理");
		var item = inventory.get(id);
		if (item == null || !item.getCharacterId().equals(context.id()) || item.getSlot() == null
				|| item.getQuantity() != 1 || item.getCustodyQuantity() != 0 || item.getPendingQuantity() != 0)
			throw new BusinessException(409, "装备不存在或当前不可强化");
		var cost = EnhancementRules.quote(catalog.definition(), item.getRequiredLevel(), item.getQuality(),
				item.getEnhancementLevel());
		CultivationMaterialMode mode = requestedMode == null
				? (item.isBound() ? CultivationMaterialMode.BOUND_FIRST : CultivationMaterialMode.UNBOUND_ONLY)
				: requestedMode;
		var plan = materials.preview(owner, Map.of(cost.stone(), cost.quantity()), mode);
		var original = codec.originalStats(item);
		return new EnhancementPreview(codec.view(item), cost, mode, plan, item.isBound() || plan.bindsTarget(),
				EnhancementRules.stats(original, cost.target(), catalog.definition()),
				EnhancementRules.stats(original, cost.failureLevel(), catalog.definition()));
	}

}

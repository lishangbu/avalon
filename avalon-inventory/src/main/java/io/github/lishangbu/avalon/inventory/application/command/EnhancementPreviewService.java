package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.item.EnhancementCatalog;
import io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService;
import io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService.Mode;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.domain.EnhancementRules;
import io.github.lishangbu.avalon.inventory.persistence.mapper.InventoryStorageCodec;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.Map;

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
	public Access access(long owner) {
		var character = characterQueries.find(owner);
		if (character == null)
			return new Access(false, catalog.definition().playerLevel());
		var adventure = characterQueries.adventure(owner);
		return new Access(character.level() >= catalog.definition().playerLevel()
				&& catalog.definition().locations().contains(character.location()) && adventure.battle() == null
				&& adventure.dungeon() == null, catalog.definition().playerLevel());
	}

	@Transactional
	public Preview preview(long owner, long id, Mode requestedMode) {
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
		Mode mode = requestedMode == null ? (item.isBound() ? Mode.BOUND_FIRST : Mode.UNBOUND_ONLY) : requestedMode;
		var plan = materials.preview(owner, Map.of(cost.stone(), cost.quantity()), mode);
		var original = codec.originalStats(item);
		return new Preview(codec.view(item), cost, mode, plan, item.isBound() || plan.bindsTarget(),
				EnhancementRules.stats(original, cost.target(), catalog.definition()),
				EnhancementRules.stats(original, cost.failureLevel(), catalog.definition()));
	}

	public record Preview(InventoryItemView before, EnhancementRules.Quote cost, Mode mode,
			CultivationMaterialService.Plan materials, boolean resultBound, Map<String, Integer> successStats,
			Map<String, Integer> failureStats) {
		public Preview {
			successStats = Map.copyOf(successStats);
			failureStats = Map.copyOf(failureStats);
		}
	}

	public record Access(boolean available, int requiredLevel) {
	}

}

package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.item.model.EnhancementDefinition;
import io.github.lishangbu.avalon.content.item.model.EnhancementFee;
import io.github.lishangbu.avalon.content.item.model.EnhancementStone;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 强化数值随启动目录固定，缺失费用档不推断为免费或沿用上一档。 */
@Component
public class EnhancementCatalog {

	private final EnhancementDefinition definition;

	public EnhancementCatalog(WorldCatalog world, ItemCatalog items) {
		definition = ContentLoader.read("/content/enhancement.json", EnhancementDefinition.class);
		validate(definition);
		definition.locations().forEach(world::place);
		definition.stones().forEach(stone -> {
			if (items.item(stone.item()).task())
				throw new IllegalArgumentException("强化石不能是任务物品");
		});
	}

	/** 启动与配置测试共用校验，避免重叠范围导致同一件装备有两个价格或材料档。 */
	public static void validate(EnhancementDefinition value) {
		if (value.playerLevel() < 1 || value.locations().isEmpty() || value.maximumLevel() != 7
				|| value.successPercent().size() != value.maximumLevel() || value.statPercentPerLevel() <= 0
				|| value.fees().isEmpty() || value.stones().isEmpty()
				|| !value.qualityMultipliers().keySet().equals(Set.of("普通", "优秀", "精良", "史诗", "传说")))
			throw new IllegalArgumentException("强化目录不完整");
		for (int probability : value.successPercent())
			if (probability < 1 || probability > 100)
				throw new IllegalArgumentException("强化概率无效");
		value.qualityMultipliers().values().forEach(multiplier -> {
			if (multiplier == null || multiplier.signum() <= 0)
				throw new IllegalArgumentException("强化品质系数无效");
		});
		int last = 0;
		for (EnhancementFee fee : value.fees()) {
			if (fee.minimum() != last + 1 || fee.maximum() < fee.minimum() || fee.gold() <= 0)
				throw new IllegalArgumentException("强化费用档必须有序连续且为正数");
			last = fee.maximum();
		}
		last = 0;
		for (EnhancementStone stone : value.stones()) {
			if (stone.minimum() != last + 1 || stone.maximum() != stone.minimum() + 19 || stone.item() == null
					|| stone.item().isBlank())
				throw new IllegalArgumentException("强化石档必须连续且每档20级");
			last = stone.maximum();
		}
		if (last < value.fees().getLast().maximum())
			throw new IllegalArgumentException("强化材料未覆盖费用范围");
	}

	public EnhancementDefinition definition() {
		return definition;
	}

}

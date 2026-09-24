package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 强化数值随启动目录固定，缺失费用档不推断为免费或沿用上一档。 */
@Component
public class EnhancementCatalog {

	private final Definition definition;

	public EnhancementCatalog(WorldCatalog world, ItemCatalog items) {
		definition = ContentLoader.read("/content/enhancement.json", Definition.class);
		validate(definition);
		definition.locations().forEach(world::place);
		definition.stones().forEach(stone -> {
			if (items.item(stone.item()).task())
				throw new IllegalArgumentException("强化石不能是任务物品");
		});
	}

	/** 启动与配置测试共用校验，避免重叠范围导致同一件装备有两个价格或材料档。 */
	public static void validate(Definition value) {
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
		for (Fee fee : value.fees()) {
			if (fee.minimum() != last + 1 || fee.maximum() < fee.minimum() || fee.gold() <= 0)
				throw new IllegalArgumentException("强化费用档必须有序连续且为正数");
			last = fee.maximum();
		}
		last = 0;
		for (Stone stone : value.stones()) {
			if (stone.minimum() != last + 1 || stone.maximum() != stone.minimum() + 19 || stone.item() == null
					|| stone.item().isBlank())
				throw new IllegalArgumentException("强化石档必须连续且每档20级");
			last = stone.maximum();
		}
		if (last < value.fees().getLast().maximum())
			throw new IllegalArgumentException("强化材料未覆盖费用范围");
	}

	public Definition definition() {
		return definition;
	}

	public record Fee(int minimum, int maximum, long gold) {
	}

	public record Stone(int minimum, int maximum, String item) {
	}

	public record Definition(int playerLevel, List<String> locations, int maximumLevel, List<Integer> successPercent,
			int statPercentPerLevel, List<Fee> fees, Map<String, BigDecimal> qualityMultipliers, List<Stone> stones) {
		public Definition {
			locations = List.copyOf(locations);
			successPercent = List.copyOf(successPercent);
			fees = List.copyOf(fees);
			qualityMultipliers = Map.copyOf(qualityMultipliers);
			stones = List.copyOf(stones);
		}
	}

}

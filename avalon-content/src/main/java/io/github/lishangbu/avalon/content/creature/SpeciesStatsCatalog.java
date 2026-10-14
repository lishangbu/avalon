package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.creature.model.SpeciesStatsStats;
import io.github.lishangbu.avalon.content.creature.model.SpeciesStatsDefinition;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 幻兽逐形态的整数成长目录。登记属性不等于开放物种，获取资格仍由遭遇、配方与任务决定。 进化查询目标形态，不读取原形态面板，也不重复叠加进化倍率。
 */
@Component
public class SpeciesStatsCatalog {

	private final Map<String, SpeciesStatsStats> species;

	public SpeciesStatsCatalog() {
		this(ContentLoader.read("/content/species-stats.json", SpeciesStatsDefinition.class).species());
	}

	/** 明确输入成长快照，重放时不重新读取运行目录。 */
	public SpeciesStatsCatalog(Map<String, SpeciesStatsStats> species) {
		this.species = Map.copyOf(species);
	}

	/** 未登记的物种直接拒绝，禁止默认为100而掩盖配置缺失。 */
	public SpeciesStatsStats base(String id) {
		var value = species.get(id);
		if (value == null)
			throw new IllegalArgumentException("缺少幻兽七维基础值：" + id);
		return value;
	}

	/** 返回纯成长面板；特性、装备和战斗状态在各自阶段计算，不修改目录。 */
	public SpeciesStatsStats atLevel(String id, int level) {
		if (level < 1 || level > 50)
			throw new IllegalArgumentException("幻兽成长等级超出基础版范围");
		var b = base(id);
		long attack = 20L + 5L * level;
		return new SpeciesStatsStats(scale(30L * (level + 5), b.health()), scale(40L + 4L * level, b.mana()),
				scale(attack, b.physicalAttack()), scale(attack, b.magicAttack()), scale(attack, b.physicalDefense()),
				scale(attack, b.magicDefense()), scale(20L + 2L * level, b.speed()));
	}

	/** 正数整数除法即最终向下取整，先用long乘算，再严格检查返回范围。 */
	private static int scale(long factor, int base) {
		return Math.toIntExact(Math.multiplyExact(factor, base) / 100);
	}

}

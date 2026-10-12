package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.creature.model.EvolutionDefinition;
import io.github.lishangbu.avalon.content.creature.model.EvolutionRoute;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.HashSet;
import org.springframework.stereotype.Component;

/**
 * 进化路线的启动期定义。形态编号独立于昵称，配方不根据属性猜测，也不消耗其他幻兽。 这里只描述成长与资格，不创建个体、不发技能、不提高正式等级上限。
 */
@Component
public class EvolutionCatalog {

	private final EvolutionDefinition definition;

	public EvolutionCatalog(CreatureCatalog creatures, ItemCatalog items, BattleCatalog battle, WorldCatalog world,
			SpeciesStatsCatalog stats) {
		definition = creatures.evolutionDefinition();
		validate(definition, creatures, items, battle);
		// 配方只定义资格和消耗，源形态与目标形态必须各自具有独立七维数据。
		definition.routes().forEach(route -> {
			stats.base(route.source());
			stats.base(route.target());
		});
		if (definition.locations().isEmpty())
			throw new IllegalArgumentException("进化研究设施地点为空");
		definition.locations().forEach(world::place);
	}

	/** 支持相邻单线进化；形态目录先验证拓扑，此处检查每步材料、技能及递增等级。 */
	public static void validate(EvolutionDefinition definition, CreatureCatalog creatures, ItemCatalog items,
			BattleCatalog battle) {
		if (definition.playerLevel() < 1 || definition.routes().isEmpty())
			throw new IllegalArgumentException("进化开放等级或路线为空");
		var sources = new HashSet<String>();
		var targets = new HashSet<String>();
		for (var route : definition.routes()) {
			ContentChecks.checkId(sources, route.source());
			ContentChecks.checkId(targets, route.target());
			if (!creatures.contains(route.source()) || route.name() == null || route.name().isBlank()
					|| route.level() < 1 || route.materials().isEmpty()
					|| route.materials()
						.entrySet()
						.stream()
						.anyMatch(entry -> entry.getValue() <= 0 || !items.hasItem(entry.getKey())
								|| items.item(entry.getKey()).task())
					|| !creatures.contains(route.target()) || route.source().equals(route.target())
					|| route.level() <= creatures.pet(route.source()).level()
					|| (route.skill() != null && battle.skill(route.skill()) == null))
				throw new IllegalArgumentException("进化路线无效：" + route.source());
		}
	}

	public EvolutionDefinition definition() {
		return definition;
	}

	/** 没有配置路线表示当前不可进化，不自动匹配相同属性的其他路线。 */
	public EvolutionRoute route(String source) {
		return definition.routes().stream().filter(route -> route.source().equals(source)).findFirst().orElse(null);
	}

}

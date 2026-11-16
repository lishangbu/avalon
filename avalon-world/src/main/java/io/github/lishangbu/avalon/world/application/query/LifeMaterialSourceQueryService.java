package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.content.life.FarmingCatalog;
import io.github.lishangbu.avalon.content.life.FishingCatalog;
import io.github.lishangbu.avalon.content.life.GatheringCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 从正式生活目录反查材料来源，避免客户端维护另一份地点、配方或价格表。 本查询不读取或修改角色资产；来源存在不表示当前角色已满足地图、专业和图纸资格。
 */
@Component
@RequiredArgsConstructor
public class LifeMaterialSourceQueryService {

	private final GatheringCatalog gathering;

	private final LifeProductionCatalog production;

	private final FarmingCatalog farming;

	private final FishingCatalog fishing;

	private final WorldCatalog world;

	private final ItemCatalog items;

	/**
	 * 按采集、制造、购买、种植和钓鱼顺序给出直接来源，不递归展开原料，避免循环配方造成无限查询。
	 * 稀有材料必须明确标注随机获得，制造来源则展示专业分工，不能把来源提示理解为免费发放或必定产出。
	 */
	public List<String> sources(String code) {
		var result = new ArrayList<String>();
		for (var node : gathering.nodes()) {
			if (!code.equals(node.product()) && !code.equals(node.rare()))
				continue;
			result.add("采集：" + world.place(node.location()).name() + "·" + node.name() + "，需要"
					+ LifeSkillRulesSkill.valueOf(node.skill()).title + node.level() + "级，"
					+ toolTier(LifeSkillRules.toolTier(node.level())) + "或更高档对应采集工具"
					+ (code.equals(node.rare()) ? "；随机稀有产出，不保证每次获得" : "；普通采集产出"));
		}
		for (var recipe : production.recipes()) {
			if (!code.equals(recipe.product()))
				continue;
			result.add("制造：" + LifeSkillRulesSkill.valueOf(recipe.skill()).title + recipe.level() + "级，地点："
					+ locations(recipe.locations()) + "，需要" + toolTier(production.minimumToolTier(recipe))
					+ "或更高档对应制造工具，成功产出" + recipe.quantity() + "件"
					+ (recipe.blueprint() == null ? "" : "；须携带图纸：" + items.item(recipe.blueprint()).name())
					+ "。可由该专业玩家制造后交易，物品绑定限制仍适用");
		}
		for (var offer : production.offers()) {
			if (code.equals(offer.item()))
				result.add("生活商店：" + locations(offer.locations()) + "，每件" + offer.price() + "金币");
		}
		for (var crop : farming.definition().crops()) {
			if (code.equals(crop.product()))
				result.add("种植：" + crop.name() + "，需要种植" + crop.level() + "级，地点："
						+ locations(farming.definition().locations()) + "，使用" + items.item(crop.seed()).name());
		}
		for (var fish : fishing.definition().fish()) {
			if (code.equals(fish.id()))
				result.add("钓鱼：需要钓鱼" + fish.level() + "级，地点：" + locations(fish.locations()) + "；须准备对应鱼竿和鱼饵，成功后获得");
		}
		// 未覆盖的战斗掉落或任务来源不作猜测，明确本列表只是生活目录中的直接来源。
		if (result.isEmpty())
			result.add("生活目录中未登记直接来源，请查看任务奖励、战斗掉落或玩家市场；市场是否有货以实际挂单为准");
		return result.stream().distinct().toList();
	}

	/** 地点名称统一解析正式世界目录，避免把内部地点编号暴露为玩家操作指引。 */
	private String locations(List<String> codes) {
		return String.join("、", codes.stream().map(code -> world.place(code).name()).toList());
	}

	/** 档位沿用正式工具等级映射；仅翻译展示名称，不在查询中另行推导工具可用资格。 */
	private String toolTier(int tier) {
		return List.of("木档", "铜档", "铁档", "银档", "金档").get(tier);
	}

}

package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.life.FarmingCatalog;
import io.github.lishangbu.avalon.content.life.FishingCatalog;
import io.github.lishangbu.avalon.content.life.GatheringCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.world.application.query.LifeMaterialSourceQueryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 使用正式目录检查来源串联，防止提示使用过期地点、错误工具档位或把稀有材料标为普通产出。 */
class LifeMaterialSourceQueryTest {

	private final WorldCatalog world = new WorldCatalog();

	private final ItemCatalog items = new ItemCatalog(world);

	private final GatheringCatalog gathering = new GatheringCatalog(items, world);

	private final LifeProductionCatalog production = new LifeProductionCatalog(items, world);

	private final FarmingCatalog farming = new FarmingCatalog(items, world);

	private final FishingCatalog fishing = new FishingCatalog(items, world);

	private final LifeMaterialSourceQueryService query = new LifeMaterialSourceQueryService(gathering, production,
			farming, fishing, world, items);

	@Test
	void ordinaryAndRareGatheringRemainDistinct() {
		// 从真实资源点选择产物，测试并不注入另一份来源表；具体资格仍由生产命令校验。
		var node = gathering.nodes().stream().filter(value -> value.rare() != null).findFirst().orElseThrow();
		assertTrue(query.sources(node.product()).stream().anyMatch(value -> value.contains("普通采集产出")));
		assertTrue(query.sources(node.rare()).stream().anyMatch(value -> value.contains("随机稀有产出")));
		assertTrue(query.sources(node.product())
			.stream()
			.anyMatch(value -> value.contains(world.place(node.location()).name())));
	}

	@Test
	void toolUpgradeShowsPreviousTierAndRealShopPrice() {
		// 铜工具允许木档工具启动制造；来源提示必须与制造服务使用的正式档位规则一致。
		assertTrue(query.sources("TOOL_COPPER_NEEDLE")
			.stream()
			.anyMatch(value -> value.contains("木档") && value.contains("制药3级")));
		var offer = production.offers()
			.stream()
			.filter(value -> value.item().equals("TOOL_WOOD_NEEDLE"))
			.findFirst()
			.orElseThrow();
		assertTrue(query.sources(offer.item()).stream().anyMatch(value -> value.contains("每件" + offer.price() + "金币")));
	}

	@Test
	void cropsFishAndUnregisteredSourcesHaveExplicitGuidance() {
		assertTrue(query.sources(farming.definition().crops().getFirst().product())
			.stream()
			.anyMatch(value -> value.startsWith("种植：")));
		assertTrue(query.sources(fishing.definition().fish().getFirst().id())
			.stream()
			.anyMatch(value -> value.startsWith("钓鱼：")));
		// 未登记直接来源时不承诺市场有货，也不猜测战斗掉落地点。
		assertTrue(query.sources("UNREGISTERED_SOURCE").getFirst().contains("以实际挂单为准"));
	}

	@Test
	void highTierGatheringExplainsStoryAccessWithoutExposingInternalFactCodes() {
		var sources = query.sources("LIFE_HIDE_4");
		assertTrue(sources.stream().anyMatch(value -> value.contains("晨金谷地") && value.contains("专业等级达标不代表地图已解锁")));
		assertTrue(sources.stream().noneMatch(value -> value.contains("QUEST:") || value.contains("J025")));
	}

}

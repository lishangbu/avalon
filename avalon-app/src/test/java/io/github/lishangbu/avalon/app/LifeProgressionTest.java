package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.life.FarmingCatalog;
import io.github.lishangbu.avalon.content.life.FishingCatalog;
import io.github.lishangbu.avalon.content.life.GatheringCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.world.domain.FishingRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

/** 验证正式内容配合衰减规则可持续升级；材料及工具由合作玩家提供，不代表个人自给路线。 */
class LifeProgressionTest {

	@Test
	void fishingProgressionUsesObservedTrackingOutcomesWithoutPredictingFutureFishPositions() throws Exception {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var fishing = new FishingCatalog(items, world);
		var production = new LifeProductionCatalog(items, world);
		int baitPrice = production.offers()
			.stream()
			.filter(o -> o.item().equals(fishing.definition().bait()))
			.findFirst()
			.orElseThrow()
			.price();
		var rows = new ArrayList<FishStage>();
		for (int responseMillis : List.of(100, 250, 500)) {
			long points = 0;
			for (int level = 1; level < 10; level++) {
				int currentLevel = level;
				var fish = fishing.definition()
					.fish()
					.stream()
					.filter(f -> f.level() == currentLevel)
					.max(Comparator.comparingInt(FishingCatalog.Fish::points))
					.orElseThrow();
				int successes = 0;
				long elapsed = 0;
				for (int sample = 0; sample < 256; sample++) {
					long seed = sample * 7919L + 31;
					var state = FishingRules.start(0);
					for (int time = 0; time <= FishingRules.MAX_MILLIS
							&& state.status().equals("PLAYING"); time += responseMillis) {
						// 只读取上次已确认状态的鱼区，不提前读取下一时刻轨迹。
						boolean held = state.position() < FishingRules.fishPosition(seed, fish.difficulty(),
								state.elapsedMillis());
						state = FishingRules.advance(state, time, held, true, seed, fish.difficulty(), level,
								(level - 1) / 2);
					}
					assertNotEquals("PLAYING", state.status());
					if (state.status().equals("CAUGHT"))
						successes++;
					elapsed += state.elapsedMillis();
				}
				int needed = 0;
				while (LifeSkillRules.level(points) == level) {
					points = LifeSkillRules.gain(points, fish.level(), fish.points());
					needed++;
				}
				if (responseMillis == 100)
					assertEquals(256, successes, "正常心跳跟踪应能完成适龄鱼种");
				assertTrue(successes >= 244, "适龄鱼种在500毫秒以内观察间隔应保留至少95%的跟踪成功样本");
				double probability = successes / 256.0;
				Double attempts = successes == 0 ? null : needed / probability;
				rows.add(new FishStage(responseMillis, level, fish.id(), successes, needed, elapsed / 256000.0,
						attempts, attempts == null ? null : attempts * elapsed / 256000.0,
						attempts == null ? null : attempts * baitPrice));
			}
		}
		assertEquals(27, rows.size());
		Files.createDirectories(Path.of("target"));
		Files
			.writeString(Path.of("target/fishing-progression.json"), new ObjectMapper().writerWithDefaultPrettyPrinter()
				.writeValueAsString(new FishReport("每级256种固定种子、适龄最高奖励鱼种、最低档鱼竿；100/250/500毫秒根据上次服务端状态跟踪。"
						+ "这是规则控制器样本，不是真人成功率或真实网络延迟。保留升级溢出；零成功情景不外推有限升级时间。" + "在线时间仅含小游戏，不含抛竿操作、转场、购饵、背包、工具补造或专业解锁。",
						rows)));
	}

	private record FishStage(int responseMillis, int fromLevel, String fish, int successesOutOf256,
			int catchesToNextLevel, double meanAttemptSeconds, Double expectedAttempts, Double stageSeconds,
			Double baitGold) {
	}

	private record FishReport(String scope, List<FishStage> stages) {
	}

	@Test
	void farmingGrowthTimeAccountsForParallelPlotsAndMidHarvestPromotion() throws Exception {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var farming = new FarmingCatalog(items, world);
		var production = new LifeProductionCatalog(items, world);
		var rows = new ArrayList<FarmStage>();
		for (int plots : List.of(6, 24)) {
			long points = 0, seconds = 0, previousPromotion = 0, gold = 0;
			int cycles = 0;
			while (LifeSkillRules.level(points) < 10) {
				int level = LifeSkillRules.level(points);
				// 同等每小时奖励选择短周期，减少升级后尚未收获的低级作物数量。
				var crop = farming.definition()
					.crops()
					.stream()
					.filter(c -> c.level() == level)
					.min(Comparator.comparingDouble((FarmingCatalog.Crop c) -> (double) c.growthSeconds() / c.points())
						.thenComparingInt(FarmingCatalog.Crop::growthSeconds))
					.orElseThrow();
				int price = production.offers()
					.stream()
					.filter(o -> o.item().equals(crop.seed()))
					.findFirst()
					.orElseThrow()
					.price();
				gold += (long) price * plots;
				seconds += crop.growthSeconds();
				cycles++;
				for (int plot = 0; plot < plots; plot++) {
					int before = LifeSkillRules.level(points);
					points = LifeSkillRules.gain(points, crop.level(), crop.points());
					if (LifeSkillRules.level(points) > before) {
						rows.add(new FarmStage(plots, before, seconds - previousPromotion, seconds, cycles, gold));
						previousPromotion = seconds;
					}
				}
				assertTrue(cycles < 10000);
			}
			assertEquals(LifeSkillRules.MAX_HALF_POINTS, points);
		}
		assertEquals(18, rows.size());
		for (int stage = 0; stage < 9; stage++)
			assertTrue(rows.get(stage + 9).cumulativeSeconds() <= rows.get(stage).cumulativeSeconds());
		Files.createDirectories(Path.of("target"));
		Files.writeString(Path.of("target/farming-progression.json"), new ObjectMapper()
			.writerWithDefaultPrettyPrinter()
			.writeValueAsString(new FarmReport("按适龄作物最佳熟练度/生长时间、同效率选短周期；整田同步播种浇水，成熟立即逐格收获。"
					+ "升级后剩余旧作物按正式规则衰减；仅生长时间，不含购买种子、工具报废、操作延迟、库存与离线错过成熟。" + "金币为种子累计支出，不含24格扩建1800金币。初始工具和种子资金假定可用。",
					rows)));
	}

	private record FarmStage(int plots, int fromLevel, long stageSeconds, long cumulativeSeconds, int cumulativeCycles,
			long cumulativeSeedGold) {
	}

	private record FarmReport(String scope, List<FarmStage> stages) {
	}

	@Test
	void everySkillCanReachTheCapUsingUnlockedContent() throws Exception {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var production = new LifeProductionCatalog(items, world);
		var gathering = new GatheringCatalog(items, world);
		var farming = new FarmingCatalog(items, world);
		var fishing = new FishingCatalog(items, world);
		var rows = new ArrayList<Stage>();
		for (var skill : LifeSkillRules.skills()) {
			var content = new ArrayList<Activity>();
			production.recipes()
				.stream()
				.filter(r -> r.skill().equals(skill.name()))
				.forEach(r -> content.add(new Activity(r.id(), r.level(), r.points())));
			gathering.nodes()
				.stream()
				.filter(n -> n.skill().equals(skill.name()))
				.forEach(n -> content.add(new Activity(n.id(), n.level(), 1)));
			if (skill == LifeSkillRules.Skill.FARMING)
				farming.definition().crops().forEach(c -> content.add(new Activity(c.id(), c.level(), c.points())));
			if (skill == LifeSkillRules.Skill.FISHING)
				fishing.definition().fish().forEach(f -> content.add(new Activity(f.id(), f.level(), f.points())));
			long current = 0;
			for (int level = 1; level <= 10; level++) {
				int stage = level;
				var activity = content.stream()
					.filter(c -> c.level() == stage)
					.max(Comparator.comparingInt(Activity::points))
					.orElseThrow(() -> new AssertionError(skill + " 缺少 " + stage + " 级内容"));
				assertTrue(activity.points() > 0, activity.id());
				assertEquals(level, LifeSkillRules.level(current));
				if (level == 10) {
					assertEquals(LifeSkillRules.MAX_HALF_POINTS,
							LifeSkillRules.gain(current, activity.level(), activity.points()));
					continue;
				}
				long start = current;
				int successes = 0;
				while (LifeSkillRules.level(current) == level) {
					long next = LifeSkillRules.gain(current, activity.level(), activity.points());
					assertTrue(next > current, skill + " " + activity.id());
					current = next;
					assertTrue(++successes <= 32768, "升级操作次数失控");
				}
				assertEquals(level + 1, LifeSkillRules.level(current));
				rows.add(new Stage(skill.name(), level, activity.id(), activity.points(), successes, start, current));
			}
		}
		assertEquals(90, rows.size());
		var report = new Report("各级选择单次熟练度最高的适龄内容，保留溢出熟练度；次数不是耗时。" + "假定材料、工具及地图可用，不含补给、报废、失败、交易和专业学习成本。种植一次指一格收获。",
				rows);
		Files.createDirectories(Path.of("target"));
		Files.writeString(Path.of("target/life-progression.json"),
				new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report));
	}

	private record Activity(String id, int level, int points) {
	}

	private record Stage(String skill, int level, String activity, int points, int successes, long startHalfPoints,
			long endHalfPoints) {
	}

	private record Report(String scope, List<Stage> stages) {
	}

}

package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.creature.model.CreaturePet;
import io.github.lishangbu.avalon.content.creature.model.EvolutionRoute;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 连续进化拓扑测试：输入顺序不影响注册，错误链必须在启动而非玩家消耗材料后暴露。 */
class EvolutionChainTest {

	private Map<String, CreaturePet> base() {
		return new HashMap<>(
				Map.of("CLAW_CAT", new CreaturePet("CLAW_CAT", "利爪猫", "水", 3, "A", "完好防护", List.of("水弹", "微疗"), null)));
	}

	private EvolutionRoute route(String source, String target, int level) {
		return new EvolutionRoute(source, target, target, level, Map.of("MATERIAL", 2), "水弹");
	}

	@Test
	void unorderedChainRegistersBothFormsWithoutAddingSkillsOrChangingBase() {
		var entries = base();
		var evolved = new HashSet<String>();
		CreatureCatalog.appendEvolutionForms(entries,
				List.of(route("STREAM_CLAW_CAT", "TIDAL_CLAW_CAT", 20), route("CLAW_CAT", "STREAM_CLAW_CAT", 12)),
				evolved);
		assertEquals(3, entries.size());
		assertEquals(Set.of("STREAM_CLAW_CAT", "TIDAL_CLAW_CAT"), evolved);
		assertEquals(3, entries.get("CLAW_CAT").level());
		assertEquals(12, entries.get("STREAM_CLAW_CAT").level());
		assertEquals(20, entries.get("TIDAL_CLAW_CAT").level());
		assertEquals(entries.get("CLAW_CAT").skills(), entries.get("TIDAL_CLAW_CAT").skills());
		assertEquals("水", entries.get("TIDAL_CLAW_CAT").element());
	}

	@Test
	void everyEvolutionStepPreservesAllLearnableSkills() {
		var routes = List.of(route("CLAW_CAT", "MID", 12), route("MID", "END", 20));
		var pools = new HashMap<>(Map.of("CLAW_CAT", List.of("水弹", "微疗"), "MID", List.of("水弹", "微疗", "光幕"), "END",
				List.of("水弹", "微疗", "光幕")));
		assertDoesNotThrow(() -> LearningCatalog.validateEvolutionPools(pools, routes));
		pools.put("END", List.of("水弹", "微疗"));
		assertThrows(IllegalArgumentException.class, () -> LearningCatalog.validateEvolutionPools(pools, routes));
		pools.remove("MID");
		assertThrows(IllegalArgumentException.class, () -> LearningCatalog.validateEvolutionPools(pools, routes));
	}

	@Test
	void evolutionWithoutNewSkillStillRequiresCompleteInheritedPool() {
		var route = new EvolutionRoute("CLAW_CAT", "STREAM_CLAW_CAT", "溪爪猫", 12,
				Map.of("WATER_PEARL", 2, "TIDAL_CLAW_CAT_ESSENCE", 2), null);
		var pools = new HashMap<>(Map.of("CLAW_CAT", List.of("水弹", "微疗"), "STREAM_CLAW_CAT", List.of("水弹", "微疗")));
		assertDoesNotThrow(() -> LearningCatalog.validateEvolutionPools(pools, List.of(route)));
		pools.put("STREAM_CLAW_CAT", List.of("水弹"));
		assertThrows(IllegalArgumentException.class,
				() -> LearningCatalog.validateEvolutionPools(pools, List.of(route)));
	}

	@Test
	void rejectsCyclesMissingSourcesDuplicateTargetsAndNonIncreasingLevels() {
		var invalid = List.of(List.of(route("MID", "END", 20), route("END", "MID", 12)),
				List.of(route("MISSING", "END", 20)), List.of(route("CLAW_CAT", "MID", 12), route("MID", "MID", 20)),
				List.of(route("CLAW_CAT", "MID", 12), route("MID", "END", 12)),
				List.of(route("CLAW_CAT", "MID", 12), route("CLAW_CAT", "END", 20)),
				List.of(route("CLAW_CAT", "MID", 12), route("OTHER", "MID", 20)));
		for (var routes : invalid)
			assertThrows(IllegalArgumentException.class,
					() -> CreatureCatalog.appendEvolutionForms(base(), routes, new HashSet<>()));
	}

}

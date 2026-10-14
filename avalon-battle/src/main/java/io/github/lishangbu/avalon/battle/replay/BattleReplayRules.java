package io.github.lishangbu.avalon.battle.replay;

import io.github.lishangbu.avalon.battle.domain.*;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.*;
import io.github.lishangbu.avalon.content.battle.model.*;
import io.github.lishangbu.avalon.content.creature.*;
import io.github.lishangbu.avalon.content.creature.model.*;
import io.github.lishangbu.avalon.content.item.*;
import io.github.lishangbu.avalon.content.item.model.ItemDefinition;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.world.model.WorldDefinition;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 战斗记录的规则内容来源；离线装配不启动Spring、不连接数据库，也不读取当前版本的内容替代缺失快照。 */
public final class BattleReplayRules {

	/** 规则执行及引用校验实际使用的内容闭包，缺任意一份均拒绝重放。 */
	private static final Map<String, Class<?>> CONTENT = Map.ofEntries(Map.entry("opening", WorldDefinition.class),
			Map.entry("items", ItemDefinition.class), Map.entry("creatures", CreatureDefinition.class),
			Map.entry("evolution", EvolutionDefinition.class), Map.entry("quests", QuestContentDefinition.class),
			Map.entry("species-stats", SpeciesStatsDefinition.class), Map.entry("wilds", WildDefinition.class),
			Map.entry("battle", BattleDefinition.class), Map.entry("training", TrainingDefinition.class),
			Map.entry("academy-training", AcademyTrainingDefinition.class),
			Map.entry("dungeon", DungeonDefinition.class), Map.entry("ability-slots", AbilitySlotsDefinition.class));

	private BattleReplayRules() {
	}

	/** 从正式打包资源复制文本，记录保持自包含；不引入运行时热更或多个配置版本目录。 */
	public static Map<String, String> capture() {
		var content = new TreeMap<String, String>();
		for (String name : CONTENT.keySet()) {
			String path = "/content/" + name + ".json";
			try (var stream = BattleReplayRules.class.getResourceAsStream(path)) {
				if (stream == null)
					throw new IllegalStateException("战斗规则缺少内容：" + path);
				content.put(name, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
			}
			catch (IOException exception) {
				throw new IllegalStateException("读取战斗规则失败：" + path, exception);
			}
		}
		return Map.copyOf(content);
	}

	/** 使用相同构造校验明确装配纯规则；所有目录均来自记录，集合之外的配置不会被偷偷补齐。 */
	public static BattleEngine assemble(Map<String, String> content) {
		if (!content.keySet().equals(CONTENT.keySet()))
			throw new IllegalArgumentException("战斗规则快照不完整或包含未知目录");
		var world = new WorldCatalog(read(content, "opening", WorldDefinition.class));
		var items = new ItemCatalog(read(content, "items", ItemDefinition.class), world);
		var creatures = new CreatureCatalog(read(content, "creatures", CreatureDefinition.class),
				read(content, "evolution", EvolutionDefinition.class));
		var quests = new QuestCatalog(read(content, "quests", QuestContentDefinition.class), world, items, creatures);
		var species = new SpeciesStatsCatalog(read(content, "species-stats", SpeciesStatsDefinition.class).species());
		var wilds = new WildCatalog(read(content, "wilds", WildDefinition.class), creatures, items);
		var battles = new BattleCatalog(read(content, "battle", BattleDefinition.class), creatures, items, world,
				quests, wilds, species);
		var balls = new CaptureBallCatalog(items);
		var rules = new BattleRules(battles, balls, species);
		var trainingCatalog = new TrainingCatalog(read(content, "training", TrainingDefinition.class), battles);
		var academy = new AcademyTrainingCatalog(read(content, "academy-training", AcademyTrainingDefinition.class),
				world, battles, items);
		var abilities = new CreatureAbilityRules(
				new AbilitySlotsCatalog(read(content, "ability-slots", AbilitySlotsDefinition.class), creatures));
		var training = new TrainingPolicy(trainingCatalog, rules, academy, battles, creatures, abilities);
		var attacks = new DirectAttackResolver(rules, training);
		var dungeon = new DungeonCatalog(read(content, "dungeon", DungeonDefinition.class), creatures, quests, items,
				world);
		var dungeonPolicy = new DungeonBattlePolicy(dungeon, creatures, rules, attacks);
		var policies = new BattleEnemyPolicies(List.of(
				new WildEnemyPolicy(battles, new WildActionPolicy(battles), dungeonPolicy),
				new TrainingEnemyPolicy(training, trainingCatalog, academy, attacks),
				new DungeonEnemyPolicy(dungeonPolicy), new CaveEnemyPolicy(battles, new CaveBossPolicy(), attacks)));
		return new BattleEngine(new PotionRecoveryRules(items), battles, rules, balls, training,
				new BattleSkillEffects(attacks, battles, List.of()), policies, List.of());
	}

	/** 文本解析执行严格Schema验证，不接受旧技能字段或未知字段。 */
	private static <T> T read(Map<String, String> content, String name, Class<T> type) {
		String value = content.get(name);
		if (value == null || value.isBlank())
			throw new IllegalArgumentException("战斗规则快照缺失：" + name);
		return ContentLoader.parse("/content/" + name + ".json", value, type);
	}

}

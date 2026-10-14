package io.github.lishangbu.avalon.app;

import java.util.List;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.CaveEnemyPolicy;
import io.github.lishangbu.avalon.battle.domain.DungeonEnemyPolicy;
import io.github.lishangbu.avalon.battle.domain.TrainingEnemyPolicy;
import io.github.lishangbu.avalon.battle.domain.WildEnemyPolicy;
import io.github.lishangbu.avalon.battle.domain.BattleEnemyPolicies;
import io.github.lishangbu.avalon.battle.domain.BattleSkillEffects;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.CaveBossPolicy;
import io.github.lishangbu.avalon.battle.domain.DirectAttackResolver;
import io.github.lishangbu.avalon.battle.domain.DungeonBattlePolicy;
import io.github.lishangbu.avalon.battle.domain.DungeonProgressPolicy;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundCoordinator;
import io.github.lishangbu.avalon.battle.domain.WildGeneration;
import io.github.lishangbu.avalon.content.item.EquipmentSetCatalog;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.inventory.domain.EquipmentLoadoutRules;
import io.github.lishangbu.avalon.battle.domain.TrainingPolicy;
import io.github.lishangbu.avalon.battle.domain.WildActionPolicy;
import io.github.lishangbu.avalon.content.battle.AcademyTrainingCatalog;
import io.github.lishangbu.avalon.content.creature.AbilitySlotsCatalog;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.CaptureBallCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureAbilityRules;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonChestCatalog;
import io.github.lishangbu.avalon.content.item.EnhancementCatalog;
import io.github.lishangbu.avalon.content.life.GatheringCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRules;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.creature.SpeciesStatsCatalog;
import io.github.lishangbu.avalon.content.battle.TrainingCatalog;
import io.github.lishangbu.avalon.content.battle.WildCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;

/** 纯规则测试显式组装正式实现；每条用例独立实例，无 Spring、数据库或全局替身。 */
final class RuleTestFixture {

	final WorldCatalog world = new WorldCatalog();

	final ItemCatalog items = new ItemCatalog(world);

	final CreatureCatalog creatures = new CreatureCatalog();

	final QuestCatalog quests = new QuestCatalog(world, items, creatures);

	final SpeciesStatsCatalog species = new SpeciesStatsCatalog();

	final WildCatalog wilds = new WildCatalog(creatures, items);

	final BattleCatalog battles = new BattleCatalog(creatures, items, world, quests, wilds, species);

	final CaptureBallCatalog balls = new CaptureBallCatalog(items);

	final BattleRules rules = new BattleRules(battles, balls, species);

	final TrainingCatalog trainingCatalog = new TrainingCatalog(battles);

	final AcademyTrainingCatalog academy = new AcademyTrainingCatalog(world, battles, items);

	final TrainingPolicy training = new TrainingPolicy(trainingCatalog, rules, academy, battles, creatures,
			new CreatureAbilityRules(new AbilitySlotsCatalog(creatures)));

	final DirectAttackResolver attacks = new DirectAttackResolver(rules, training);

	final DungeonCatalog dungeon = new DungeonCatalog(creatures, quests, items, world);

	final DungeonBattlePolicy dungeonPolicy = new DungeonBattlePolicy(dungeon, creatures, rules, attacks);

	final BattleEnemyPolicies enemies = new BattleEnemyPolicies(List.of(
			new WildEnemyPolicy(battles, new WildActionPolicy(battles), dungeonPolicy),
			new TrainingEnemyPolicy(training, trainingCatalog, academy, attacks), new DungeonEnemyPolicy(dungeonPolicy),
			new CaveEnemyPolicy(battles, new CaveBossPolicy(), attacks)));

	final BattleEngine engine = new BattleEngine(new PotionRecoveryRules(items), battles, rules, balls, training,
			new BattleSkillEffects(attacks, battles, List.of()), enemies, List.of());

	final DungeonProgressPolicy progress = new DungeonProgressPolicy(dungeon);

	final DungeonChestCatalog chests = new DungeonChestCatalog(items, dungeon);

	final LifeProductionCatalog production = new LifeProductionCatalog(items, world);

	final GatheringCatalog gathering = new GatheringCatalog(items, world);

	final EnhancementCatalog enhancement = new EnhancementCatalog(world, items);

	final WildActionPolicy wildActions = new WildActionPolicy(battles);

	final WildGeneration generation = new WildGeneration(battles, creatures,
			new CreatureAbilityRules(new AbilitySlotsCatalog(creatures)), wilds, rules);

	final LearningCatalog learning = new LearningCatalog(items, creatures, battles);

	final EquipmentLoadoutRules loadouts = new EquipmentLoadoutRules(new EquipmentSetCatalog(items));

	final GrowthRules growth = new GrowthRules();

	final DungeonRoundCoordinator rounds = new DungeonRoundCoordinator(engine, dungeon);

}

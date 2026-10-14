package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.model.BattleDefinition;
import io.github.lishangbu.avalon.content.battle.model.BattleEncounter;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import io.github.lishangbu.avalon.content.battle.model.BattleSkillEffect;
import io.github.lishangbu.avalon.content.battle.model.BattleLowHealthPower;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.SpeciesStatsCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleDiagnosticsTest {

	private final WorldCatalog world = new WorldCatalog();

	private final ItemCatalog items = new ItemCatalog(world);

	private final CreatureCatalog creatures = new CreatureCatalog();

	private final QuestCatalog quests = new QuestCatalog(world, items, creatures);

	private final WildCatalog wilds = new WildCatalog(creatures, items);

	private final SpeciesStatsCatalog stats = new SpeciesStatsCatalog();

	private final BattleDefinition raw = ContentLoader.read("/content/battle.json", BattleDefinition.class);

	@Test
	void encounterReferenceErrorsIdentifySourceAndField() {
		var base = raw.encounters().getFirst();
		for (String field : List.of("location", "requires", "captureRequires", "wild")) {
			var encounter = new BattleEncounter("BROKEN", base.name(),
					field.equals("location") ? "MISSING" : base.location(),
					field.equals("requires") ? "MISSING" : base.requires(),
					field.equals("captureRequires") ? "MISSING" : base.captureRequires(), false, base.species(), null,
					field.equals("wild") ? "MISSING" : base.wild(), base.count());
			var error = assertThrows(IllegalArgumentException.class,
					() -> load(new BattleDefinition(raw.growth(), raw.skills(), List.of(encounter), raw.products(),
							raw.elements(), raw.supplyDrops(), raw.bookDrops())));
			assertTrue(error.getMessage().contains("/content/battle.json [BROKEN] " + field + "=MISSING"),
					error.getMessage());
		}
	}

	@Test
	void duplicateSkillsAndOverflowingDropWeightsHaveContentContext() {
		var skill = raw.skills().getFirst();
		var duplicate = assertThrows(IllegalArgumentException.class,
				() -> load(new BattleDefinition(raw.growth(), List.of(skill, skill), raw.encounters(), raw.products(),
						raw.elements(), raw.supplyDrops(), raw.bookDrops())));
		assertTrue(duplicate.getMessage().contains("skills.id=" + skill.id()));
		var overflow = assertThrows(IllegalArgumentException.class,
				() -> load(new BattleDefinition(raw.growth(), raw.skills(), raw.encounters(), raw.products(),
						raw.elements(), Map.of("NONE", Integer.MAX_VALUE, "HEALTH_POTION", 1), raw.bookDrops())));
		assertTrue(overflow.getMessage().contains("/content/battle.json"));
		assertTrue(overflow.getMessage().contains("supplyDrops."));
		assertTrue(overflow.getMessage().contains("总权重"));
		var catalog = load(raw);
		var missing = assertThrows(IllegalArgumentException.class, () -> catalog.growth("MISSING"));
		assertTrue(missing.getMessage().contains("growth=MISSING"));
	}

	/** 低血替代威力不得削弱组合中的后续攻击，校验必须覆盖全部直接伤害效果。 */
	@Test
	void lowHealthPowerChecksEveryAttackEffect() {
		for (String kind : List.of("DAMAGE", "SLOW", "POISON", "AREA")) {
			var skill = new BattleSkill("组合校验",
					List.of(new BattleSkillEffect("DAMAGE", 10), new BattleSkillEffect(kind, 200)), "无", "PA", "PD", 0,
					0, 0, "ENEMY", new BattleLowHealthPower(50, 100), null);
			var error = assertThrows(IllegalArgumentException.class, () -> load(withSkill(skill)));
			assertTrue(error.getMessage().contains("lowHealthPower"), error.getMessage());
		}
	}

	/** 治疗数值不属于攻击威力；把治疗放在前面不能使合法增威力配置失效。 */
	@Test
	void lowHealthPowerDoesNotCompareSupportEffectPower() {
		var skill = new BattleSkill("组合校验",
				List.of(new BattleSkillEffect("HEAL", 300), new BattleSkillEffect("DAMAGE", 50)), "无", "PA", "PD", 0, 0,
				0, "ENEMY", new BattleLowHealthPower(50, 100), null);
		assertNotNull(load(withSkill(skill)).skill(skill.id()));
	}

	/** 保留正式引用和成长，将额外技能加入目录以单独观察组合条件诊断。 */
	private BattleDefinition withSkill(BattleSkill skill) {
		var skills = new ArrayList<>(raw.skills());
		skills.add(skill);
		return new BattleDefinition(raw.growth(), skills, raw.encounters(), raw.products(), raw.elements(),
				raw.supplyDrops(), raw.bookDrops());
	}

	private BattleCatalog load(BattleDefinition definition) {
		return new BattleCatalog(definition, creatures, items, world, quests, wilds, stats);
	}

}

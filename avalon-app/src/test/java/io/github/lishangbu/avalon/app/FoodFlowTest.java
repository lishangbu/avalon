package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.FoodCommandService;
import io.github.lishangbu.avalon.battle.application.model.FoodRequest;
import io.github.lishangbu.avalon.battle.application.query.FoodQueryService;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.LifeCraftingService;
import io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService;
import io.github.lishangbu.avalon.world.application.model.LifeCraftingRequest;
import io.github.lishangbu.avalon.world.application.model.LifeSkillRequest;
import io.github.lishangbu.avalon.world.domain.LifeToolRandom;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;

class FoodFlowTest extends SpringIntegrationTest {

	@MockitoBean
	LifeToolRandom random;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService heroes;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService bag;

	@Autowired
	LifeSkillCommandService skills;

	@Autowired
	LifeCraftingService crafting;

	@Autowired
	FoodCommandService food;

	@Autowired
	FoodQueryService views;

	@Autowired
	CombatVitalsStore vitals;

	@Autowired
	JdbcClient jdbc;

	private long owner() {
		String name = "food_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Food-test-12345", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.grantExperience(owner, 7380);
			creatures.grant(owner, "FOOD_PET", "SPROUT");
			jdbc.sql("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
				.param("id", characters.lock(owner).id())
				.update();
		});
		return owner;
	}

	private String item(long owner, String code) {
		return bag.list(owner).stream().filter(value -> value.code().equals(code)).findFirst().orElseThrow().id();
	}

	@Test
	void boundFishBecomesOilAndThenReagentWithoutDuplicateConsumption() {
		long owner = owner();
		jdbc.sql("UPDATE player_character SET location='GNOME_APOTHECARY' WHERE id=:id")
			.param("id", heroes.requireId(owner))
			.update();
		skills.change(owner, new LifeSkillRequest(UUID.randomUUID().toString(), LifeSkillRulesSkill.ALCHEMY, false),
				false);
		jdbc.sql("UPDATE life_skill SET half_points=256 WHERE character_id=:id AND skill='ALCHEMY'")
			.param("id", heroes.requireId(owner))
			.update();
		transactions.run(() -> inventory.grant(owner, "BOUND_FISH", "FISH_0_2", 1, false));
		grant(owner, "LIFE_HERB_0", 2);
		var extract = new LifeCraftingRequest(UUID.randomUUID().toString(), "EXTRACT_FISH_0_2", 1,
				item(owner, "TOOL_WOOD_ROD"), 1, Map.of(item(owner, "FISH_0_2"), 1));
		var first = crafting.craft(owner, extract);
		assertEquals(1, first.produced());
		assertEquals(first, crafting.craft(owner, extract));
		assertTrue(bag.list(owner).stream().noneMatch(value -> value.code().equals("FISH_0_2")));
		assertTrue(bag.list(owner)
			.stream()
			.filter(value -> value.code().equals("FISH_OIL_0"))
			.allMatch(value -> value.bound()));
		var reagent = new LifeCraftingRequest(UUID.randomUUID().toString(), "OIL_REAGENT_0", 1,
				item(owner, "TOOL_WOOD_ROD"), 1, Map.of(item(owner, "FISH_OIL_0"), 1, item(owner, "LIFE_HERB_0"), 2));
		assertEquals(2, crafting.craft(owner, reagent).produced());
		assertTrue(bag.list(owner)
			.stream()
			.noneMatch(value -> value.code().equals("FISH_OIL_0") || value.code().equals("LIFE_HERB_0")));
		var result = bag.list(owner)
			.stream()
			.filter(value -> value.code().equals("LIFE_REAGENT_0"))
			.findFirst()
			.orElseThrow();
		assertEquals(2, result.quantity());
		assertTrue(result.bound());
	}

	private void grant(long owner, String code, int count) {
		transactions
			.run(() -> inventory.grantWithBinding(owner, UUID.randomUUID().toString(), code, count, false, false));
	}

	@Test
	void farmIngredientsCookIntoFoodThatRestoresActualHeroResourcesOnlyOnce() {
		long owner = owner(), hero = heroes.requireId(owner);
		jdbc.sql("UPDATE player_character SET location='GNOME_KITCHEN' WHERE id=:id").param("id", hero).update();
		skills.change(owner, new LifeSkillRequest(UUID.randomUUID().toString(), LifeSkillRulesSkill.COOKING, false),
				false);
		grant(owner, "CROP_0_0", 2);
		grant(owner, "LIFE_MEAT_0", 1);
		var cooked = crafting.craft(owner, new LifeCraftingRequest(UUID.randomUUID().toString(), "FOOD_0_0", 1,
				item(owner, "TOOL_WOOD_POT"), 1, Map.of(item(owner, "CROP_0_0"), 2, item(owner, "LIFE_MEAT_0"), 1)));
		assertEquals(1, cooked.produced());
		transactions.run(() -> {
			characters.lock(owner);
			vitals.saveVitals(hero, hero, 1, 0);
		});
		var request = new FoodRequest(UUID.randomUUID().toString(), item(owner, "FOOD_0_0"), Long.toString(hero));
		var result = food.eat(owner, request);
		assertEquals(120, result.restoredHealth());
		assertEquals(0, result.restoredMana());
		assertEquals(result, food.eat(owner, request));
		assertEquals(121,
				views.view(owner)
					.targets()
					.stream()
					.filter(value -> value.id().equals(Long.toString(hero)))
					.findFirst()
					.orElseThrow()
					.health());
		assertTrue(bag.list(owner).stream().noneMatch(value -> value.code().equals("FOOD_0_0")));
	}

	@Test
	void fullResourcesForeignTargetAndDispatchedSessionDoNotConsumeFood() {
		long owner = owner(), other = owner();
		grant(owner, "FOOD_0_0", 2);
		String item = item(owner, "FOOD_0_0"), hero = Long.toString(heroes.requireId(owner));
		assertThrows(BusinessException.class,
				() -> food.eat(owner, new FoodRequest(UUID.randomUUID().toString(), item, hero)));
		assertThrows(BusinessException.class, () -> food.eat(owner,
				new FoodRequest(UUID.randomUUID().toString(), item, Long.toString(heroes.requireId(other)))));
		transactions.run(() -> {
			characters.lock(owner);
			vitals.saveVitals(Long.parseLong(hero), Long.parseLong(hero), 1, 0);
			characters.beginLifeActivity(owner, UUID.randomUUID().toString(), null);
		});
		assertThrows(BusinessException.class,
				() -> food.eat(owner, new FoodRequest(UUID.randomUUID().toString(), item, hero)));
		assertEquals(2,
				bag.list(owner).stream().filter(value -> value.id().equals(item)).findFirst().orElseThrow().quantity());
	}

	@Test
	void foodCapsAtPetMaximumAndDoesNotReviveFallenTarget() {
		long owner = owner(), hero = heroes.requireId(owner);
		String pet = pets.list(owner).getFirst().id();
		grant(owner, "FOOD_4_0", 2);
		grant(owner, "FOOD_4_1", 1);
		transactions.run(() -> {
			characters.lock(owner);
			vitals.saveVitals(hero, Long.parseLong(pet), 0, 0);
		});
		assertThrows(BusinessException.class,
				() -> food.eat(owner, new FoodRequest(UUID.randomUUID().toString(), item(owner, "FOOD_4_0"), pet)));
		transactions.run(() -> {
			characters.lock(owner);
			vitals.saveVitals(hero, Long.parseLong(pet), 1, 0);
		});
		var result = food.eat(owner, new FoodRequest(UUID.randomUUID().toString(), item(owner, "FOOD_4_0"), pet));
		var current = views.view(owner)
			.targets()
			.stream()
			.filter(value -> value.id().equals(pet))
			.findFirst()
			.orElseThrow();
		assertEquals(current.maximumHealth(), result.health());
		assertEquals(0, result.mana());
		var mana = food.eat(owner, new FoodRequest(UUID.randomUUID().toString(), item(owner, "FOOD_4_1"), pet));
		assertEquals(current.maximumMana(), mana.mana());
	}

}

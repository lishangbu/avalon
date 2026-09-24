package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 准备事实来自真实命令，查询、越权与失败事务都不能冒充玩家已经操作。 */
class PreparationActionFactsTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactQueryService facts;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	private long player() {
		String name = "fact_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Action-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.grantExperience(owner, 700);
			creatures.grant(owner, "TEST:STARTER", "CLAW_CAT");
			inventory.grant(owner, "TEST:GEAR", "STARTER_WEAPON", 1, true);
		});
		return owner;
	}

	@Test
	void queriesNeverCreateFactsAndSamePositionIsAnExplicitSave() {
		long owner = player();
		var pet = pets.list(owner).getFirst();
		var gear = items.list(owner).stream().filter(i -> i.code().equals("STARTER_WEAPON")).findFirst().orElseThrow();
		assertFalse(facts.codes(owner).contains("ACTION:PARTY_SAVED"));
		assertFalse(facts.codes(owner).contains("ACTION:CREATURE_SKILLS_VIEWED"));
		assertFalse(facts.codes(owner).contains("ACTION:EQUIPMENT_EQUIPPED"));
		creatures.position(owner, Long.parseLong(pet.id()), pet.partyPosition());
		assertTrue(facts.codes(owner).contains("ACTION:PARTY_SAVED"));
		creatures.viewedSkills(owner, Long.parseLong(pet.id()));
		creatures.viewedSkills(owner, Long.parseLong(pet.id()));
		assertTrue(facts.codes(owner).contains("ACTION:CREATURE_SKILLS_VIEWED"));
		inventory.equip(owner, Long.parseLong(gear.id()));
		assertTrue(facts.codes(owner).contains("ACTION:EQUIPMENT_EQUIPPED"));
		assertTrue(items.list(owner).stream().anyMatch(i -> i.id().equals(gear.id()) && i.equipped()));
	}

	@Test
	void foreignAndInvalidCommandsDoNotCreateFacts() {
		long owner = player(), other = player();
		long foreignPet = Long.parseLong(pets.list(other).getFirst().id());
		long foreignGear = Long.parseLong(items.list(other)
			.stream()
			.filter(i -> i.code().equals("STARTER_WEAPON"))
			.findFirst()
			.orElseThrow()
			.id());
		assertThrows(BusinessException.class, () -> creatures.position(owner, foreignPet, 0));
		assertThrows(BusinessException.class, () -> creatures.viewedSkills(owner, foreignPet));
		assertThrows(BusinessException.class, () -> inventory.equip(owner, foreignGear));
		assertTrue(facts.codes(owner).stream().noneMatch(code -> code.startsWith("ACTION:")));
	}

	@Test
	void factsRollBackTogetherWithTheirActions() {
		long owner = player();
		long pet = Long.parseLong(pets.list(owner).getFirst().id());
		long gear = Long.parseLong(items.list(owner)
			.stream()
			.filter(i -> i.code().equals("STARTER_WEAPON"))
			.findFirst()
			.orElseThrow()
			.id());
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			creatures.position(owner, pet, 1);
			creatures.viewedSkills(owner, pet);
			inventory.equip(owner, gear);
			throw new IllegalStateException("rollback");
		}));
		assertTrue(facts.codes(owner).stream().noneMatch(code -> code.startsWith("ACTION:")));
		assertEquals(0, pets.list(owner).getFirst().partyPosition());
		assertTrue(items.list(owner).stream().noneMatch(i -> i.equipped()));
	}

}

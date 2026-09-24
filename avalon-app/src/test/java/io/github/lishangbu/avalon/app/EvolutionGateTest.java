package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionPreviewService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 研究教学前的角色不能绕过进化门槛，畸形命令也不得写入回执。 */
class EvolutionGateTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	GrowthRules growth;

	@Autowired
	CreatureEvolutionPreviewService queries;

	@Autowired
	CreatureEvolutionCommandService commands;

	@Autowired
	EntityManager em;

	@Test
	void levelBeforeResearchTeachingRejectsEvolutionAndMalformedCommandDoesNotCreateAReceipt() {
		String name = "evogate_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Evolution-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> characters.grantExperience(owner, Math.toIntExact(growth.threshold(10))));
		assertEquals(10, transactions.call(() -> characters.lock(owner).level()));
		var error = assertThrows(BusinessException.class, () -> queries.preview(owner, 1, null));
		assertTrue(error.getMessage().contains("等级"));
		assertThrows(BusinessException.class, () -> commands.evolve(owner, 1, null));
		assertThrows(BusinessException.class, () -> commands.evolve(owner, 1,
				new CreatureEvolutionCommandService.Request(UUID.randomUUID().toString(), null)));
		transactions.run(() -> assertEquals(0L,
				((Number) em
					.createNativeQuery("SELECT count(*) FROM creature_evolution_receipt WHERE character_id = :id")
					.setParameter("id", characters.lock(owner).id())
					.getSingleResult()).longValue()));
	}

}

package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.model.CreatureFormChanged;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 同步通知的数据库资源边界；不冒充尚未接入的正式进化提交或 20 级成长验收。 */
class CreatureFormVitalsTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	BattleRules rules;

	@Autowired
	CombatVitalsStore combatVitalsStore;

	@Autowired
	CombatVitalsRepository combatVitalsRepository;

	@Autowired
	ApplicationEventPublisher changed;

	private long owner;

	private CreatureFormChanged ready() {
		// 测试账号使用数字隔开前缀和随机串，避免严格短词在拼接处随机命中。
		String name = "form_0" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Form-test-1234", "测试");
		owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		return transactions.call(() -> {
			long character = characters.lock(owner).id();
			long pet = creatures.capture(owner, "FORM_TEST", "GALE_SPARROW", 8, "H", "乘风", List.of("风刃", "逆风"));
			return new CreatureFormChanged(character, pet, 8, "GALE_SPARROW", "GALE_FALCON");
		});
	}

	private void fire(CreatureFormChanged event) {
		transactions.run(() -> {
			characters.lock(owner);
			changed.publishEvent(event);
		});
	}

	@Test
	void absentArchiveUsesOldMaximumAndNeverRefillsOnRepeatedNotification() {
		var event = ready();
		var before = rules.stats("GALE_SPARROW", 8, false, Map.of());
		fire(event);
		fire(event);
		transactions.run(() -> {
			var stored = combatVitalsRepository.get(event.creatureId());
			assertEquals(before.health(), stored.getHp());
			assertEquals(before.mana(), stored.getMp());
			assertEquals(event.characterId(), stored.getCharacterId());
		});
	}

	@Test
	void zeroHealthStaysZeroAndResourceChangesRollBackWithOuterCommand() {
		var event = ready();
		transactions.run(() -> combatVitalsStore.saveVitals(event.characterId(), event.creatureId(), 0, 3));
		fire(event);
		transactions.run(() -> {
			assertEquals(0, combatVitalsRepository.get(event.creatureId()).getHp());
			assertEquals(3, combatVitalsRepository.get(event.creatureId()).getMp());
			combatVitalsStore.saveVitals(event.characterId(), event.creatureId(), 9999, 9999);
		});
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			characters.lock(owner);
			changed.publishEvent(event);
			throw new IllegalStateException("模拟进化命令后续失败");
		}));
		transactions.run(() -> assertEquals(9999, combatVitalsRepository.get(event.creatureId()).getHp()));
		fire(event);
		var maximum = rules.stats(event.species(), 8, false, Map.of());
		transactions.run(() -> {
			assertEquals(maximum.health(), combatVitalsRepository.get(event.creatureId()).getHp());
			assertEquals(maximum.mana(), combatVitalsRepository.get(event.creatureId()).getMp());
		});
	}

}

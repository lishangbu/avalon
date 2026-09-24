package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.application.query.CreatureResourceQueryService;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.creature.application.port.CreatureResourceCommand;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 生产复用战斗资源存档和角色锁，不能通过切换玩法恢复生命魔法或消费他人伙伴。 */
@Component
@RequiredArgsConstructor
public class ProductionCreatureResourceService implements CreatureResourceCommand {

	private final CharacterCommandService characters;

	private final CreatureQueryService creatures;

	private final CreatureResourceQueryService resources;

	private final CombatVitalsStore vitals;

	@Override
	@Transactional(Transactional.TxType.MANDATORY)
	public void restoreForProduction(long owner, long creature, int health, int mana) {
		if (health < 0 || mana < 0)
			throw new IllegalArgumentException("恢复量无效");
		var context = characters.requireOutsideDungeon(owner);
		var pet = creatures.list(owner)
			.stream()
			.filter(value -> value.id().equals(Long.toString(creature)))
			.findFirst()
			.orElseThrow(() -> new BusinessException(404, "伙伴不存在"));
		var current = resources.read(context.id(), creature, pet.species(), pet.level());
		vitals.saveVitals(context.id(), creature, (int) Math.min(current.health(), (long) current.hp() + health),
				(int) Math.min(current.mana(), (long) current.mp() + mana));
	}

	@Override
	@Transactional(Transactional.TxType.MANDATORY)
	public boolean spendForProduction(long owner, long creature, int health, int mana) {
		if (health <= 0 || mana < 0)
			throw new IllegalArgumentException("生产资源消耗无效");
		var context = characters.requireOutsideDungeon(owner);
		var pet = creatures.list(owner)
			.stream()
			.filter(value -> value.id().equals(Long.toString(creature)))
			.findFirst()
			.orElseThrow(() -> new BusinessException(404, "伙伴不存在或已不在身边"));
		var current = resources.read(context.id(), creature, pet.species(), pet.level());
		if (current.hp() <= health || current.mp() < mana)
			return false;
		vitals.saveVitals(context.id(), creature, current.hp() - health, current.mp() - mana);
		return true;
	}

}

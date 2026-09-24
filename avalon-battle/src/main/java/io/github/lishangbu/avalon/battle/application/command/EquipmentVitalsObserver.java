package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.application.model.PartyFactory;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.EquipmentResourceRules;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.inventory.application.model.EquipmentChanged;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** 战斗模块维护资源存档；库存模块只发送同步通知，不反向依赖战斗实体。 */
@Component
@RequiredArgsConstructor
public class EquipmentVitalsObserver {

	private final PartyFactory party;

	private final BattleRules rules;

	private final CombatVitalsStore combatVitalsStore;

	private final CombatVitalsRepository combatVitalsRepository;

	/**
	 * 没有资源存档时以换装前上限为基准，避免首次穿戴自动补满新增生命或魔力。 已有存档只向下截断，零生命仍为零。同步执行确保连续卸装、穿回无法恢复资源。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	@EventListener
	public void changed(EquipmentChanged event) {
		var hero = party.create(event.owner(), false).getFirst();
		long id = Long.parseLong(hero.getId());
		var before = rules.stats(hero.getSpecies(), hero.getLevel(), true, event.previousStats());
		var stored = combatVitalsRepository.get(id);
		int hp = stored == null ? before.health() : stored.getHp();
		int mp = stored == null ? before.mana() : stored.getMp();
		combatVitalsStore.saveVitals(id, id, EquipmentResourceRules.clamp(hp, hero.getHp()),
				EquipmentResourceRules.clamp(mp, hero.getMp()));
	}

}

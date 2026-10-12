package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.application.model.DungeonEncounterFinished;
import io.github.lishangbu.avalon.battle.domain.DungeonRewardRules;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonRewardCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.inventory.application.command.DungeonSupplyCommandService;
import io.github.lishangbu.avalon.inventory.application.command.DungeonChestCommandService;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.SplittableRandom;

/**
 * 在持有副本、战局和成员锁的结算事务内发奖。参与凭证、金币、经验和补给共同回滚； 使用战局种子与角色编号派生掉落，数据库失败后重试不会重新抽取结果。
 */
@Component
@RequiredArgsConstructor
public class DungeonRewardObserver {

	private final DungeonCatalog dungeons;

	private final DungeonRewardCatalog rewards;

	private final DungeonRewardRules rules;

	private final CharacterCommandService characters;

	private final CharacterFactCommandService facts;

	private final CharacterFactQueryService factQueries;

	private final CreatureCommandService creatures;

	private final DungeonSupplyCommandService supplies;

	private final DungeonChestCommandService chests;

	@Transactional(Transactional.TxType.MANDATORY)
	@EventListener
	public void reward(DungeonEncounterFinished event) {
		var battle = event.result().getBattle();
		if (!"WON".equals(battle.getStatus()))
			return;
		var stage = dungeons.stage(battle.getDungeonStage());
		boolean complete = stage.id().equals(dungeons.definition().stages().getLast().id());
		String day = "DUNGEON:DAILY:" + dungeons.definition().id() + ":" + rules.day(Instant.now());
		for (var entry : event.result().getMembers().entrySet()) {
			var participation = entry.getValue();
			var member = event.progress().getMembers().get(entry.getKey());
			if (!participation.isParticipated() || participation.isRemoved() || participation.isLeaving()
					|| member == null || member.isExited())
				continue;
			long owner = member.getOwner();
			characters.lock(owner);
			var recorded = factQueries.codes(owner);
			String receipt = "DUNGEON:REWARD:" + event.battle();
			if (recorded.contains(receipt))
				continue;
			// 逐次副本记录实际参战，不以永久击败事实或最后一场的参与代替完整通关。
			String participationPrefix = "DUNGEON:PARTICIPATED:" + event.run() + ":";
			String exploration = "DUNGEON:FIRST_COMPLETE:" + dungeons.definition().id();
			boolean firstComplete = complete && !recorded.contains(exploration) && dungeons.definition()
				.stages()
				.stream()
				.allMatch(
						prior -> prior.id().equals(stage.id()) || recorded.contains(participationPrefix + prior.id()));
			int explorationExperience = firstComplete ? rewards.definition().firstCompletionExperience() : 0;
			var hero = battle.getUnits()
				.stream()
				.filter(unit -> unit.getId().equals(entry.getKey()))
				.findFirst()
				.orElseThrow();
			int experience = rules.experience(stage, hero.getLevel(), complete) + explorationExperience;
			characters.grantExperience(owner, experience);
			var petExperience = new HashMap<Long, Integer>();
			for (var unit : battle.getUnits()) {
				Integer position = member.getRoster().get(unit.getId());
				if (!unit.isEnemy() && !unit.isHero() && entry.getKey().equals(unit.getOwnerHero()) && position != null
						&& position >= 0 && position < 4)
					petExperience.put(Long.parseLong(unit.getId()),
							rules.experience(stage, unit.getLevel(), complete) + explorationExperience);
			}
			creatures.grantBattleExperience(owner, petExperience);
			int gold = stage.gold() * Math.max(1, stage.wildSpecies().size());
			characters.changeGold(owner, gold);
			if (rewards.definition().supplyStages().contains(stage.id())) {
				var random = new SplittableRandom(battle.getSeed() ^ Long.parseLong(entry.getKey()));
				for (int index = 0; index < Math.max(1, stage.wildSpecies().size()); index++) {
					String code = rules.supply(random);
					if (!code.equals("NONE")) {
						supplies.reward(event.run(), owner, "BATTLE:" + event.battle() + ":" + index, code, 1);
						battle.getLoot().add(new BattleState.Loot(owner, code, 1));
					}
				}
			}
			if (complete && !recorded.contains(day)) {
				characters.changeGold(owner, rewards.definition().dailyGold());
				rewards.definition()
					.dailySupplies()
					.forEach((code, amount) -> supplies.reward(event.run(), owner, day + ":" + code, code, amount));
				facts.record(owner, day);
				battle.getLog().add(hero.getName() + "领取今日首次参与礼包。");
			}
			if (complete && entry.getKey().equals(event.progress().getLeader())) {
				chests.award(owner, event.run(), battle.getSeed());
				battle.getLog().add(hero.getName() + "获得发起者宝箱，可离开副本后在背包领取。");
			}
			facts.record(owner, receipt);
			facts.record(owner, participationPrefix + stage.id());
			if (firstComplete) {
				facts.record(owner, exploration);
				battle.getLog().add(hero.getName() + "完成首次完整探索，额外获得经验 " + explorationExperience + "（满级截断）。");
			}
			// 只有本场实际参战者分享击杀计数，捕获及仍存活的目标不计入，回执重放在上游已退出。
			for (var enemy : battle.getUnits())
				if (enemy.isEnemy() && !enemy.isCaptured() && enemy.getHp() == 0 && enemy.getSpecies() != null
						&& !enemy.getSpecies().isBlank())
					facts.record(owner, "KILL:" + enemy.getSpecies());
			// 固定关卡事实供任务配置引用；只在本场合法参战者首次结算时发布，回执重放不能补记任务。
			facts.record(owner, "VICTORY:" + stage.id());
			battle.getLog().add(hero.getName() + "获得经验 " + experience + "（满级截断），金币 " + gold + "。");
		}
	}

}

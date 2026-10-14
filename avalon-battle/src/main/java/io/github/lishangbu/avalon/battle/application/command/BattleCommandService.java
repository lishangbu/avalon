package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.application.model.BattleTurnPayload;

import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.application.model.BattleViews;
import io.github.lishangbu.avalon.battle.application.model.PartyFactory;
import io.github.lishangbu.avalon.battle.application.query.EncounterAccess;
import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleEngineResources;
import io.github.lishangbu.avalon.battle.domain.BattleResourceSnapshot;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateLoot;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.BattleStateStats;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.EvolutionMaterialDrops;
import io.github.lishangbu.avalon.battle.domain.WildGeneration;
import io.github.lishangbu.avalon.battle.persistence.entity.Battle;
import io.github.lishangbu.avalon.battle.persistence.entity.BattleTurn;
import io.github.lishangbu.avalon.battle.persistence.entity.SupplyPurchase;
import io.github.lishangbu.avalon.battle.persistence.repository.BattleRecordRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.BattleTurnRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.battle.persistence.repository.SupplyPurchaseRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.CaptureBallCatalog;
import io.github.lishangbu.avalon.content.battle.WildCatalog;
import io.github.lishangbu.avalon.content.battle.model.BattleEncounter;
import io.github.lishangbu.avalon.content.creature.EvolutionDropCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.util.*;
import lombok.RequiredArgsConstructor;
import io.github.lishangbu.avalon.battle.replay.BattleReplayJournal;
import io.github.lishangbu.avalon.battle.replay.BattleReplayCodec;
import io.github.lishangbu.avalon.battle.domain.BattleRoundResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 战斗命令的事务边界。锁角色后串行执行开战、回合、捕获及结算，最后一起提交存档和回执。
 */
@Component
@RequiredArgsConstructor
public class BattleCommandService {

	/** 文本记录只在事务提交后落盘，不参与数据库资产写入。 */
	private final BattleReplayJournal journal;

	private final CharacterCommandService characters;

	private final CharacterFactCommandService facts;

	private final CharacterFactQueryService factQueries;

	private final QuestCommandService questCommands;

	private final CharacterQueryService characterQueries;

	private final CreatureCommandService creatures;

	private final CreatureQueryService creatureQueries;

	private final InventoryCommandService inventory;

	private final QuestQueryService quests;

	private final EncounterAccess encounterAccess;

	private final BattleCatalog catalog;

	private final CaptureBallCatalog balls;

	private final ItemCatalog items;

	private final BattleRecordRepository battleRecordRepository;

	private final SupplyPurchaseRepository supplyPurchaseRepository;

	private final BattleTurnRepository battleTurnRepository;

	private final CombatVitalsStore combatVitalsStore;

	private final CombatVitalsRepository combatVitalsRepository;

	private final ObjectMapper codec;

	private final BattleRules rules;

	private final BattleEngine engine;

	private final BattleViews views;

	private final PartyFactory party;

	private final TrainingCommandService training;

	private final WorldCatalog world;

	private final WildCatalog wilds;

	private final EvolutionDropCatalog evolutionDrops;

	private final WildGeneration wildGeneration;

	private final SecureRandom entropy = new SecureRandom();

	@Transactional
	public BattleView start(long owner, String encounterId, String requestId) {
		requireKey(requestId);
		var context = characters.lock(owner);
		var prior = battleRecordRepository.request(context.id(), requestId);
		if (prior != null) {
			var state = codec.readValue(prior.getState(), BattleState.class);
			if (!state.getEncounter().equals(encounterId))
				throw new BusinessException(409, "请求号已用于另一遭遇");
			return views.view(owner, prior.getId(), state);
		}
		characters.requireOutsideDungeon(owner);
		var encounter = catalog.encounter(encounterId);
		if (encounter == null || !encounter.location().equals(context.location())
				|| !encounterAccess.allowed(owner, encounter)
				|| (encounter.wild() != null && context.level() < wilds.profile(encounter.wild()).minPlayerLevel()))
			throw new BusinessException(409, "当前地点尚未开放此遭遇");
		if (encounterId.equals("CAVE_BOSS") && encounterAccess.cavePrepared(owner) && !quests.done(owner, "BQ011")) {
			facts.record(owner, "READINESS:BQ011_CONFIRMED");
			// 开战本身就是玩家确认准备完毕的动作；把 BQ011 的经验和金币
			// 放在同一事务中结算，避免只写准备事实却遗漏任务奖励。
			questCommands.interact(owner, "BQ011");
		}
		if (encounter.tutorial() && creatureQueries.hasSourcePrefix(owner, encounter.id() + ":"))
			throw new BusinessException(409, "已捕获教学伙伴，请编队后回报村长");
		if (encounter.boss() != null && factQueries.codes(owner).contains("VICTORY:" + encounterId))
			throw new BusinessException(409, "已经击败此剧情首领，请继续后续任务");
		var state = new BattleState();
		state.setSeed(entropy.nextLong());
		state.setEncounter(encounterId);
		state.setCaptureAllowed(encounter.boss() == null && captureRequirementMet(owner, encounter));
		state.setUnits(party.create(owner, false));
		for (var unit : state.getUnits()) {
			var vitals = combatVitalsRepository.get(Long.parseLong(unit.getId()));
			if (vitals != null) {
				unit.setHp(Math.min(unit.getHp(), vitals.getHp()));
				unit.setMp(Math.min(unit.getMp(), vitals.getMp()));
			}
			combatVitalsStore.saveVitals(context.id(), Long.parseLong(unit.getId()), unit.getHp(), unit.getMp());
		}
		if (state.getUnits().stream().noneMatch(BattleStateUnit::alive))
			throw new BusinessException(409, "队伍已倒下，请先到幻兽中心恢复");
		var random = new Random(state.getSeed());
		if (encounter.boss() != null) {
			var template = encounter.boss();
			var enemy = new BattleStateUnit();
			enemy.setId("boss");
			enemy.setEnemy(true);
			enemy.setName(template.name());
			enemy.setSpecies(template.policy());
			enemy.setElement(template.element());
			enemy.setLevel(template.level());
			enemy.setStats(new BattleStateStats(template.health(), 0, template.attack(), template.attack(),
					template.defense(), template.defense(), template.speed()));
			enemy.setHp(template.health());
			state.getUnits().add(enemy);
		}
		else {
			state.getUnits().addAll(wildGeneration.create(encounter, random));
		}
		engine.planEnemies(state);
		state.getLog().add("进入" + encounter.name() + "。");
		var battle = new Battle();

		battle.setCharacterId(context.id());
		battle.setRequestId(requestId);
		battle.setActive(true);
		battle.setState(codec.writeValueAsString(state));
		battleRecordRepository.saveAndFlush(battle);
		journal.start("SINGLE", battle.getId(), state);
		characters.setBattle(owner, battle.getId(), false);
		return views.view(owner, battle.getId(), state);
	}

	/** 请求号、修订和完整载荷共同确认幂等；同号异参拒绝，已完成旧请求返回保存结果。 */
	@Transactional
	public BattleView turn(long owner, long id, UUID requestId, int revision, int round, List<BattleStateOrder> orders,
			boolean surrender, Map<String, Integer> replacements) {
		if (requestId == null || revision < 0 || round < 1 || orders == null || replacements == null)
			throw new BusinessException(400, "回合请求号、修订或指令格式无效");
		var payload = new BattleTurnPayload(requestId, revision, round, orders, surrender, replacements);
		String canonical = BattleReplayCodec.write(payload);
		var context = characters.lock(owner);
		var battle = battleRecordRepository.get(id);
		if (battle == null || !battle.getCharacterId().equals(context.id()))
			throw new BusinessException(404, "战局不存在");
		var receipt = battleTurnRepository.request(id, requestId.toString());
		if (receipt != null) {
			if (!receipt.getPayload().equals(canonical))
				throw new BusinessException(409, "同一请求号不能提交不同指令");
			return codec.readValue(receipt.getResponse(), BattleView.class);
		}
		if (battleTurnRepository.turn(id, round) != null)
			throw new BusinessException(409, "本轮已由其他请求完成，请刷新战局");
		var state = codec.readValue(battle.getState(), BattleState.class);
		if (!battle.isActive() || state.getRound() != round || state.getRevision() != revision)
			throw new BusinessException(409, "回合已变化，请刷新战局");
		var resources = new BattleEngineResources() {
			public int count(BattleStateUnit actor, String code) {
				if (state.getTraining() != null && state.getTraining().capturePractice())
					return state.getTraining().getSupplies().getOrDefault(code, 0);
				return views.supplies(owner).getOrDefault(code, 0);
			}

			public boolean room(BattleStateUnit target) {
				if (state.getTraining() != null && state.getTraining().capturePractice())
					return state.getUnits().stream().noneMatch(BattleStateUnit::isCaptured);
				return views.room(owner, context.level(), target.getLevel());
			}

			public void take(BattleStateUnit actor, String code) {
				if (state.getTraining() != null && state.getTraining().capturePractice()) {
					int remaining = count(actor, code);
					if (remaining <= 0)
						throw new BusinessException(409, "训练物品不足，请退出后免费重开");
					state.getTraining().getSupplies().put(code, remaining - 1);
					return;
				}
				inventory.take(owner, code, 1);
			}

			public void capture(BattleStateUnit target) {
				// 捕获结果保留在战局目标上，不能触碰真实幻兽、图鉴奖励或战后资源存档。
				if (state.getTraining() != null && state.getTraining().capturePractice())
					return;
				long petId = creatures.capture(owner, state.getEncounter() + ":" + id + ":" + target.getId(),
						target.getSpecies(), target.getLevel(), target.getAbilitySlot(), target.getAbility(),
						target.getSkills());
				// 捕获保留当前剩余资源；城镇恢复是显式操作，不让新个体无条件满血满魔。
				var normal = rules.stats(target.getSpecies(), target.getLevel(), false, Map.of());
				combatVitalsStore.saveVitals(context.id(), petId, Math.min(target.getHp(), normal.health()),
						Math.min(target.getMp(), normal.mana()));
			}
		};
		// 纯核心先完成整轮，之后才在当前角色锁事务内应用资源；任何后续失败都会回滚全部资产。
		var snapshot = BattleResourceSnapshot.read(state, orders, resources);
		var transition = engine.advance(state, orders, surrender, snapshot,
				replacements == null ? Map.of() : replacements);
		var evidence = BattleReplayCodec.read(BattleReplayCodec.write(transition), BattleRoundResult.class);
		state.copyFrom(transition.state());
		for (var change : transition.changes()) {
			var actor = state.getUnits()
				.stream()
				.filter(unit -> unit.getId().equals(change.owner()))
				.findFirst()
				.orElseThrow();
			if (change.kind().equals("CONSUME"))
				resources.take(actor, change.reference());
			else if (change.kind().equals("CAPTURE"))
				resources.capture(state.getUnits()
					.stream()
					.filter(unit -> unit.getId().equals(change.target()))
					.findFirst()
					.orElseThrow());
			else
				throw new IllegalStateException("未注册战斗资产结果：" + change.kind());
		}
		for (var u : state.getUnits())
			if (state.getTraining() == null && !u.isEnemy())
				combatVitalsStore.saveVitals(context.id(), Long.parseLong(u.getId()), u.getHp(), u.getMp());
		if (!state.getStatus().equals("ACTIVE")) {
			battle.setActive(false);
			characters.setBattle(owner, null,
					state.getTraining() == null && Set.of("LOST", "SURRENDERED").contains(state.getStatus()));
			if (state.getTraining() == null)
				settle(owner, state, id);
			else
				training.settle(owner, state);
		}
		// 每份回执保存最近战报窗口，历史回执仍保留各轮结果，避免长战局快照二次增长。
		if (state.getLog().size() > 200)
			state.setLog(new ArrayList<>(state.getLog().subList(state.getLog().size() - 200, state.getLog().size())));
		battle.setState(codec.writeValueAsString(state));
		var result = views.view(owner, id, state);
		var saved = new BattleTurn();

		saved.setBattleId(id);
		saved.setRound(round);
		saved.setRequestId(requestId.toString());
		saved.setPayload(canonical);
		saved.setResponse(codec.writeValueAsString(result));
		battleTurnRepository.save(saved);
		journal.round("SINGLE", id, requestId.toString(), evidence.before(), evidence, "ACTIVE");
		// 奖励、捕获归档和战报截断属于宿主结算，也要留下与下一轮输入衔接的正式状态。
		journal.state("SINGLE", id, "SINGLE_SETTLEMENT", evidence.state(), state);
		if (!state.getStatus().equals("ACTIVE"))
			journal.finish("SINGLE", id, state.getStatus());
		battleRecordRepository.flush();
		return result;
	}

	/**
	 * 仅最终胜利的普通击败发奖；捕获目标及教学遭遇没有击杀收益。种子派生掉落只生成一次。
	 */
	private void settle(long owner, BattleState state, long id) {
		var encounter = catalog.encounter(state.getEncounter());
		if (encounter.boss() != null) {
			if (state.getStatus().equals("WON")) {
				facts.record(owner, "VICTORY:" + encounter.id());
				if (encounter.id().equals("CAVE_BOSS")) {
					facts.record(owner, "VICTORY:BQ012_CAVE_GUARD");
				}
				// 战后任务通过统一执行器发放装备与经验，并由任务回执防止重发。
				var reward = questCommands.interact(owner, encounter.boss().rewardStep());
				if (reward != null)
					state.getLog().addAll(reward.rewards());
			}
			return;
		}
		if (!state.getStatus().equals("WON") || catalog.encounter(state.getEncounter()).tutorial())
			return;
		if (encounter.id().equals("PATH_GUARD") && quests.done(owner, "BQ026")
				&& factQueries.codes(owner).contains("SCENE:BQ027_CLEAR_PATH")) {
			facts.record(owner, "VICTORY:BQ027_PATH_GUARD");
			var reward = questCommands.interact(owner, "BQ027");
			if (reward != null)
				state.getLog().addAll(reward.rewards());
		}
		var defeated = state.getUnits().stream().filter(u -> u.isEnemy() && !u.isCaptured() && u.getHp() == 0).toList();
		// 园圃作业只要求外围恢复安全：捕捉成功也能解除危险，但不追加击杀经验或掉落。
		if (encounter.id().equals("GARDEN_WORK_ROUTE"))
			facts.record(owner, "VICTORY:GARDEN_WORK_ROUTE");
		// 普通遭遇的击败事实可供任务签收使用；单纯捕捉不冒充护送战斗胜利。
		if (!defeated.isEmpty())
			facts.record(owner, "VICTORY:" + encounter.id());
		var wild = encounter.wild() == null ? null : wilds.profile(encounter.wild());
		int multiplier = wild == null ? 1 : wild.experienceMultiplier();
		var hero = state.getUnits().stream().filter(BattleStateUnit::isHero).findFirst().orElseThrow();
		int xp = defeated.stream().mapToInt(u -> rules.experience(u.getLevel(), hero.getLevel(), multiplier)).sum();
		long before = characterQueries.find(owner).experience();
		characters.grantExperience(owner, xp);
		long actual = characterQueries.find(owner).experience() - before;
		Map<Long, Integer> petXp = new HashMap<>();
		for (var u : state.getUnits())
			if (!u.isEnemy() && !u.isHero())
				petXp.put(Long.parseLong(u.getId()),
						defeated.stream()
							.mapToInt(e -> rules.experience(e.getLevel(), u.getLevel(), multiplier))
							.sum());
		creatures.grantBattleExperience(owner, petXp);
		int goldMultiplier = wild == null ? 1 : wild.goldMultiplier();
		int gold = defeated.stream().mapToInt(u -> (u.getLevel() + 2) / 3 * goldMultiplier).sum();
		characters.changeGold(owner, gold);
		state.getLog()
			.add("战斗基础经验 " + defeated.stream().mapToInt(u -> u.getLevel() * 10 * multiplier).sum() + "，人物修正经验 " + xp
					+ "，实际入账 " + actual + "，上限舍弃 " + (xp - actual) + "；金币 +" + gold + "。");
		var random = new Random(state.getSeed() ^ 0x6A09E667F3BCC909L);
		int index = 0;
		for (var enemy : defeated) {
			// 只有最终胜利且未捕获的实际击败才累计击杀任务，战斗回执重放不会再次走结算。
			facts.record(owner, "KILL:" + enemy.getSpecies());
			String code = weighted(enemy.isElite() ? wild.supplyDrops() : catalog.definition().supplyDrops(), random);
			code = balls.dropForLevel(code, enemy.getLevel());
			if (!code.equals("NONE"))
				drop(owner, id, index++, code, state);
			String book = weighted(enemy.isElite() ? wild.bookDrops() : catalog.definition().bookDrops(), random);
			if (!book.equals("NONE"))
				drop(owner, id, index++, enemy.isElite() ? book : book.equals("BOOK")
						? catalog.growth(enemy.getSpecies()).book() : catalog.growth(enemy.getSpecies()).fragment(),
						state);
			if (!enemy.isElite()) {
				for (var material : EvolutionMaterialDrops.roll(evolutionDrops.drop(enemy.getSpecies()),
						enemy.getLevel(), random))
					drop(owner, id, index++, material.code(), material.quantity(), state);
			}
		}
	}

	/** 初次捕捉在河岸观察后开放；其余遭遇遵循目录中校验过的捕捉前置。 */
	private boolean captureRequirementMet(long owner, BattleEncounter encounter) {
		if (encounter.tutorial())
			return quests.done(owner, "BQ006");
		return encounter.captureRequires() == null || quests.done(owner, encounter.captureRequires());
	}

	/**
	 * 每池各抽一次；采用稳定编号排序，不让 Map 遍历顺序影响同种子的重试结果。
	 */
	private String weighted(Map<String, Integer> weights, Random random) {
		int roll = random.nextInt(weights.values().stream().mapToInt(Integer::intValue).sum());
		for (var entry : new TreeMap<>(weights).entrySet()) {
			roll -= entry.getValue();
			if (roll < 0)
				return entry.getKey();
		}
		throw new IllegalStateException("掉落权重无效");
	}

	private void drop(long owner, long battle, int index, String code, BattleState state) {
		drop(owner, battle, index, code, 1, state);
	}

	private void drop(long owner, long battle, int index, String code, int quantity, BattleState state) {
		inventory.grantWithBinding(owner, "BATTLE:" + battle + ":" + index, code, quantity, false, false);
		state.getLog().add("获得" + items.item(code).name() + " ×" + quantity + "。");
		state.getLoot().add(new BattleStateLoot(owner, code, quantity));
	}

	@Transactional
	public void recover(long owner) {
		var context = characters.requireOutsideDungeon(owner);
		characters.requireNoLifeActivity(owner);
		if (!world.provides(context.location(), "RECOVERY"))
			throw new BusinessException(409, "请前往幻兽中心恢复");
		for (var u : party.create(owner, true))
			combatVitalsStore.saveVitals(context.id(), Long.parseLong(u.getId()), u.getHp(), u.getMp());
		characters.invalidatePreparation(owner);
		// 教学恢复必须发生在捕捉教学完成之后，早期恢复不能提前满足庆典开路前的整备要求。
		if (quests.done(owner, "BQ007"))
			facts.record(owner, "RECOVERED_AFTER_CAPTURE");
		if (quests.done(owner, "BQ008"))
			facts.record(owner, "RECOVERY:BQ009_CHECKED");
		if (quests.done(owner, "BQ023"))
			facts.record(owner, "READINESS:BQ024_SUPPLIES_REVIEWED");
	}

	@Transactional
	public void purchase(long owner, String code, String requestId) {
		requireKey(requestId);
		var context = characters.requireOutsideDungeon(owner);
		var prior = supplyPurchaseRepository.purchase(context.id(), requestId);
		if (prior != null) {
			if (!prior.getCode().equals(code))
				throw new BusinessException(409, "购买请求号已使用");
			return;
		}
		if (!world.provides(context.location(), "SUPPLIES"))
			throw new BusinessException(409, "请到杂货铺购买已解锁补给");
		var product = catalog.productsAt(context.location(), factQueries.codes(owner))
			.stream()
			.filter(p -> p.code().equals(code))
			.findFirst()
			.orElseThrow(() -> new BusinessException(404, "商品不存在"));
		characters.changeGold(owner, -product.price());
		inventory.grantStrict(owner, "PURCHASE:" + requestId, code, 1, false, true);
		var receipt = new SupplyPurchase();

		receipt.setCharacterId(context.id());
		receipt.setCode(code);
		receipt.setRequestId(requestId);
		supplyPurchaseRepository.save(receipt);
	}

	private void requireKey(String key) {
		if (key == null || !key.matches("[a-zA-Z0-9-]{8,64}"))
			throw new BusinessException(400, "请求号无效");
	}

}

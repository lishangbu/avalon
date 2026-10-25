package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.application.model.DungeonDay;
import io.github.lishangbu.avalon.battle.application.model.DungeonEncounterFinished;
import io.github.lishangbu.avalon.battle.application.model.PartyFactory;
import io.github.lishangbu.avalon.battle.domain.*;
import io.github.lishangbu.avalon.battle.domain.BattleEngineResources;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.DungeonProgressStateMember;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundStateSubmission;
import io.github.lishangbu.avalon.battle.persistence.entity.DungeonRun;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonBattleRecordRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonDailyStartStore;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonRunRecordRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.inventory.application.command.DungeonSupplyCommandService;
import io.github.lishangbu.avalon.inventory.application.query.DungeonSupplyQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import io.github.lishangbu.avalon.battle.domain.BattleSeedSource;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 正式副本事务编排：锁顺序固定为进度、已有战局、按账号升序排列的角色。 本类尚不是匹配入口，create 只供已完成三人手动确认的匹配事务调用。
 * 单场结算、资源写回、进度与奖励通知同事务；不把读页面当作超时处理。
 */
@Component
@RequiredArgsConstructor
public class DungeonRunCommandService {

	private final DungeonRunRecordRepository dungeonRunRecordRepository;

	private final DungeonDailyStartStore dungeonDailyStartStore;

	private final DungeonBattleRecordRepository dungeonBattleRecordRepository;

	private final CombatVitalsStore combatVitalsStore;

	private final CombatVitalsRepository combatVitalsRepository;

	private final DungeonBattleCommandService battles;

	private final DungeonProgressPolicy progress;

	private final DungeonBattlePolicy policy;

	private final ObjectMapper codec;

	private final PartyFactory parties;

	private final CharacterCommandService characters;

	private final CharacterQueryService characterQueries;

	private final CharacterFactQueryService facts;

	private final DungeonSupplyCommandService supplies;

	private final DungeonSupplyQueryService supplyQueries;

	private final DungeonCatalog catalog;

	private final ApplicationEventPublisher finished;

	private final BattleSeedSource entropy;

	/** 单人入口只创建认证角色本人的进度；三人副本仍须组队确认。 */
	@Transactional
	public long solo(String dungeon, long owner, String request) {
		if (catalog.definition(dungeon).players() != 1)
			throw new BusinessException(400, "此副本需要组队确认");
		return create(dungeon, owner, List.of(owner), request);
	}

	/** 正式开本只扣发起者；相同匹配请求重试返回原进度，不重新锁队或刷新物资。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public long create(long leader, List<Long> owners, String request) {
		return create(catalog.definition().id(), leader, owners, request);
	}

	/** 显式指定副本；幂等请求必须同时匹配副本和原始成员。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public long create(String dungeon, long leader, List<Long> owners, String request) {
		var definition = catalog.definition(dungeon);
		if (request == null || !request.matches("[a-zA-Z0-9-]{8,64}") || owners == null
				|| owners.size() != definition.players() || owners.stream().anyMatch(Objects::isNull)
				|| new HashSet<>(owners).size() != owners.size() || !owners.contains(leader))
			throw new BusinessException(400, "副本成员或请求号无效");
		var ordered = owners.stream().sorted().toList();
		ordered.forEach(characters::lock);
		long leaderId = characterQueries.requireId(leader);
		var prior = dungeonRunRecordRepository.request(leaderId, request);
		if (prior != null) {
			var old = decode(prior);
			if (!old.getDungeon().equals(dungeon) || !initialOwners(old).equals(ordered))
				throw new BusinessException(409, "请求号已用于其他队伍");
			return prior.getId();
		}
		ordered.forEach(owner -> requireEligible(owner, dungeon));
		var day = DungeonDay.at(Instant.now(), definition.zone(), definition.resetHour());
		if (dungeonDailyStartStore.starts(leaderId, dungeon, day) >= definition.dailyStarts())
			throw new BusinessException(409, "今日发起次数已用完");
		var state = progress.create(dungeon, Long.toString(leaderId),
				ordered.stream().map(owner -> characterQueries.requireId(owner).toString()).toList());
		state.setInitialOwners(ordered);
		for (long owner : ordered) {
			var member = state.getMembers().get(characterQueries.requireId(owner).toString());
			prepareMember(member, owner);
		}
		var run = new DungeonRun();

		run.setLeaderId(leaderId);
		run.setRequestId(request);
		save(run, state);
		dungeonRunRecordRepository.saveAndFlush(run);
		for (long owner : ordered) {
			characters.enterDungeon(owner, run.getId());
			supplies.enter(run.getId(), owner, state.getDungeon());
		}
		dungeonDailyStartStore.useStart(leaderId, dungeon, day);
		return run.getId();
	}

	/** 只有仍在本次副本内的成员可发布或停止招募；战斗中不调整招募状态。 */
	@Transactional
	public void recruit(long runId, long owner, boolean enabled) {
		var run = requireActive(runId);
		var state = decode(run);
		requireMember(state, owner);
		if (state.getActiveBattle() != null)
			throw new BusinessException(409, "请先完成当前副本战斗");
		if (enabled && state.getMembers().values().stream().filter(member -> !member.isExited()).count() >= catalog
			.definition(state.getDungeon())
			.players())
			throw new BusinessException(409, "副本队伍已满");
		state.setRecruiting(enabled);
		save(run, state);
	}

	/** 候补确认前和实际入本前都持进度锁检查；确认不保证抢占最后一个空位。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void requireRecruiting(long runId, long owner) {
		var state = decode(requireActive(runId));
		if (!state.isRecruiting())
			throw new BusinessException(409, "副本已停止补员");
		// 在临时解码结果上校验资格，不提前登记成员或锁定编队。
		progress.join(state, characterQueries.requireId(owner).toString());
	}

	/** 已完成手动确认的补员事务调用；只补真实空位，不扣新成员次数、不重置已完成遭遇。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void join(long runId, long owner) {
		var run = requireActive(runId);
		var state = decode(run);
		characters.lock(owner);
		String id = characterQueries.requireId(owner).toString();
		var existing = state.getMembers().get(id);
		if (existing != null && !existing.isExited()) {
			if (!Objects.equals(characterQueries.adventure(owner).dungeon(), runId))
				throw new BusinessException(409, "角色副本状态不一致");
			return;
		}
		requireEligible(owner, state.getDungeon());
		// 升级前存档尚无原始名单时，在首次补员前固定，避免后续重放受新增成员影响。
		state.setInitialOwners(initialOwners(state));
		progress.join(state, id);
		prepareMember(state.getMembers().get(id), owner);
		characters.enterDungeon(owner, runId);
		supplies.enter(runId, owner, state.getDungeon());
		if (state.getMembers().values().stream().filter(member -> !member.isExited()).count() == catalog
			.definition(state.getDungeon())
			.players())
			state.setRecruiting(false);
		save(run, state);
	}

	private List<Long> initialOwners(DungeonProgressState state) {
		return state.getInitialOwners().isEmpty()
				? state.getMembers().values().stream().map(DungeonProgressStateMember::getOwner).sorted().toList()
				: state.getInitialOwners();
	}

	private void requireEligible(long owner, String dungeon) {
		var definition = catalog.definition(dungeon);
		var context = characters.requireOutsideDungeon(owner);
		if (!definition.entrance().equals(context.location()))
			throw new BusinessException(409, "请先沿地图道路抵达对应副本入口，再进入副本");
		if (context.level() < definition.level() || !facts.codes(owner).contains("QUEST:" + definition.requires()))
			throw new BusinessException(409, "队员尚未达到副本等级或前置任务要求");
	}

	/** 创建和补员共用同一入本快照规则；保留真实资源，不因新登记自动恢复。 */
	private void prepareMember(DungeonProgressStateMember member, long owner) {
		member.setOwner(owner);
		var units = parties.create(owner, true);
		var hero = units.getFirst();
		for (var unit : units) {
			if (!unit.isHero() && unit.getPosition() < 4) {
				if (unit.getLevel() > hero.getLevel())
					throw new BusinessException(409, "出战伙伴等级不能高于人物");
				member.getRoster().put(unit.getId(), unit.getPosition());
			}
			if (unit.isHero() || unit.getPosition() < 100)
				restoreVitals(unit, true);
		}
		member.setCarried(units.stream()
			.filter(unit -> !unit.isHero() && unit.getPosition() < 100)
			.map(BattleStateUnit::getId)
			.toList());
	}

	@Transactional
	public void ready(long runId, long owner) {
		var run = requireActive(runId);
		var state = decode(run);
		var id = requireMember(state, owner);
		characters.requireOutsideBattle(owner);
		progress.ready(state, id);
		state.getMembers().get(id).setPresenceUntil(Instant.now().plusSeconds(20));
		state.getMembers().get(id).setPreparedRevision(characterQueries.adventure(owner).preparationRevision());
		save(run, state);
	}

	/**
	 * 显式在线续期，不放在 GET 查询中。过期后先清除准备再续期， 因而页面重连不能自动重新准备；副本行锁与开战共用，避免检查后再被并发续期。
	 * 战斗中的断线只由提交截止处理，这里不修改已锁定战局或指令。
	 */
	@Transactional
	public void presence(long runId, long owner) {
		var run = requireActive(runId);
		var state = decode(run);
		var member = state.getMembers().get(requireMember(state, owner));
		if (state.getActiveBattle() != null)
			return;
		var now = Instant.now();
		if (!member.presentAt(now))
			member.setReady(false);
		member.setPresenceUntil(now.plusSeconds(20));
		save(run, state);
	}

	@Transactional
	public void move(long runId, long owner, int location) {
		var run = requireActive(runId);
		var state = decode(run);
		progress.move(state, requireMember(state, owner), location);
		save(run, state);
	}

	/** 休整只在实际到达的休整点恢复入本随身成员，仓库不受影响。 */
	@Transactional
	public void rest(long runId, long owner) {
		var run = requireActive(runId);
		var state = decode(run);
		var id = requireMember(state, owner);
		characters.requireOutsideBattle(owner);
		progress.rest(state, id);
		var member = state.getMembers().get(id);
		for (var unit : parties.create(owner, true))
			if (unit.isHero() || member.getCarried().contains(unit.getId()))
				combatVitalsStore.saveVitals(Long.parseLong(id), Long.parseLong(unit.getId()), unit.getHp(),
						unit.getMp());
		characters.invalidatePreparation(owner);
		save(run, state);
	}

	/** 全员最新准备后才创建一场战局；不根据刷新或到期自动开下一场。 */
	@Transactional
	public long begin(long runId, long owner) {
		var run = requireActive(runId);
		var state = decode(run);
		requireMember(state, owner);
		if (state.getActiveBattle() != null)
			return state.getActiveBattle();
		lockMembers(state);
		var now = Instant.now();
		for (var member : state.getMembers().values()) {
			if (member.isExited())
				continue;
			characters.requireOutsideBattle(member.getOwner());
			if (!member.presentAt(now) || member.getPreparedRevision() != characterQueries.adventure(member.getOwner())
				.preparationRevision())
				member.setReady(false);
		}
		String stage = progress.nextStage(state);
		var units = new ArrayList<BattleStateUnit>();
		for (var member : state.getMembers().values()) {
			if (member.isExited())
				continue;
			for (var unit : parties.create(member.getOwner(), true)) {
				if (!unit.isHero() && !member.getRoster().containsKey(unit.getId()))
					continue;
				if (!unit.isHero())
					unit.setPosition(member.getRoster().get(unit.getId()));
				restoreVitals(unit, true);
				units.add(unit);
			}
		}
		long battle = battles.create(policy.create(stage, units, entropy.nextLong()), Instant.now());
		progress.begin(state, battle);
		for (var member : state.getMembers().values())
			if (!member.isExited())
				characters.setBattle(member.getOwner(), battle, false);
		save(run, state);
		return battle;
	}

	@Transactional
	public void submit(long runId, long owner, long battle, UUID requestId, int revision, int round,
			DungeonRoundStateSubmission input) {
		var run = require(runId);
		var state = decode(run);
		String hero = requireMember(state, owner);
		// 历史相同提交仍交给战局凭证重放，但不能用其他进度的战局号读取或执行。
		if (!Objects.equals(state.getActiveBattle(), battle) && !state.getResolvedBattles().containsKey(battle))
			throw new BusinessException(409, "战局不属于当前副本进度");
		dungeonBattleRecordRepository.lock(battle);
		lockMembers(state);
		battles.submit(battle, Long.parseLong(hero), requestId, revision, round, input, Instant.now(),
				resources(runId, state));
		if (Objects.equals(state.getActiveBattle(), battle))
			sync(run, state, battle);
	}

	/** 调度器只能处理当前关联战局；持锁后使用服务器时钟，旧扫描候选不推进新战局。 */
	@Transactional
	public boolean expire(long runId, long battle) {
		var run = require(runId);
		var state = decode(run);
		if (!Objects.equals(state.getActiveBattle(), battle))
			return false;
		dungeonBattleRecordRepository.lock(battle);
		lockMembers(state);
		boolean changed = battles.expire(battle, Instant.now(), resources(runId, state));
		if (changed)
			sync(run, state, battle);
		return changed;
	}

	@Transactional
	public void leave(long runId, long owner) {
		var run = requireActive(runId);
		var state = decode(run);
		String hero = requireMember(state, owner);
		if (state.getActiveBattle() != null) {
			long battle = state.getActiveBattle();
			dungeonBattleRecordRepository.lock(battle);
			lockMembers(state);
			battles.leave(battle, Long.parseLong(hero), Instant.now(), resources(runId, state));
			sync(run, state, battle);
		}
		else {
			characters.lock(owner);
			progress.leave(state, hero);
			release(runId, owner);
			save(run, state);
		}
	}

	private void sync(DungeonRun run, DungeonProgressState state, long battle) {
		var result = BattleStateCodec.dungeon(dungeonBattleRecordRepository.get(battle).getState());
		var units = new ArrayList<>(result.getBattle().getUnits());
		result.getWithdrawn().values().forEach(units::addAll);
		for (var unit : units) {
			if (unit.isEnemy())
				continue;
			String hero = unit.isHero() ? unit.getId() : unit.getOwnerHero();
			// 先前回合已主动退出的资源不能被随后回合的 withdrawn 历史覆盖。
			if (!state.getMembers().get(hero).isExited())
				combatVitalsStore.saveVitals(Long.parseLong(hero), Long.parseLong(unit.getId()), unit.getHp(),
						unit.getMp());
		}
		boolean ended = !result.getBattle().getStatus().equals("ACTIVE");
		for (var entry : result.getMembers().entrySet()) {
			var member = state.getMembers().get(entry.getKey());
			if (member.isExited())
				continue;
			if (ended)
				characters.setBattle(member.getOwner(), null, false);
			if (entry.getValue().isLeaving() && entry.getValue().isRemoved()) {
				characters.setBattle(member.getOwner(), null, false);
				member.setExited(true);
				member.setReady(false);
				release(run.getId(), member.getOwner());
			}
		}
		if (ended && progress.resolve(state, battle, result.getBattle().getStatus())) {
			finished.publishEvent(new DungeonEncounterFinished(run.getId(), battle, state, result));
			// 同事务观察者追加的奖励战报也必须持久化，刷新不能丢失最后的结算记录。
			dungeonBattleRecordRepository.get(battle).setState(codec.writeValueAsString(result));
			if (state.getCleared() == catalog.definition(state.getDungeon()).stages().size()) {
				for (var member : state.getMembers().values())
					if (!member.isExited())
						release(run.getId(), member.getOwner());
			}
		}
		save(run, state);
	}

	private BattleEngineResources resources(long run, DungeonProgressState state) {
		return new BattleEngineResources() {
			private long owner(BattleStateUnit actor) {
				var member = state.getMembers().get(actor.isHero() ? actor.getId() : actor.getOwnerHero());
				if (member == null || member.isExited())
					throw new BusinessException(409, "行动者不属于本次副本");
				return member.getOwner();
			}

			public int count(BattleStateUnit actor, String code) {
				return supplyQueries.available(run, owner(actor)).getOrDefault(code, 0);
			}

			public void take(BattleStateUnit actor, String code) {
				supplies.take(run, owner(actor), code, 1);
			}

			public boolean room(BattleStateUnit target) {
				return false;
			}

			public void capture(BattleStateUnit target) {
				throw new BusinessException(409, "副本敌人不能捕捉");
			}
		};
	}

	private void restoreVitals(BattleStateUnit unit, boolean persist) {
		long id = Long.parseLong(unit.getId());
		var stored = combatVitalsRepository.get(id);
		if (stored != null) {
			unit.setHp(Math.min(unit.getHp(), stored.getHp()));
			unit.setMp(Math.min(unit.getMp(), stored.getMp()));
		}
		if (persist)
			combatVitalsStore.saveVitals(Long.parseLong(unit.isHero() ? unit.getId() : unit.getOwnerHero()), id,
					unit.getHp(), unit.getMp());
	}

	private void release(long run, long owner) {
		supplies.close(run, owner);
		characters.leaveDungeon(owner, run);
	}

	private void lockMembers(DungeonProgressState state) {
		state.getMembers()
			.values()
			.stream()
			.filter(member -> !member.isExited())
			.map(DungeonProgressStateMember::getOwner)
			.sorted()
			.forEach(characters::lock);
	}

	private String requireMember(DungeonProgressState state, long owner) {
		String id = characterQueries.requireId(owner).toString();
		var member = state.getMembers().get(id);
		if (member == null || member.getOwner() != owner || member.isExited())
			throw new BusinessException(404, "副本进度不存在");
		return id;
	}

	private DungeonRun require(long run) {
		var entity = dungeonRunRecordRepository.lock(run);
		if (entity == null)
			throw new BusinessException(404, "副本进度不存在");
		return entity;
	}

	private DungeonRun requireActive(long run) {
		var entity = require(run);
		if (!entity.isActive())
			throw new BusinessException(409, "副本进度已经结束");
		return entity;
	}

	private DungeonProgressState decode(DungeonRun run) {
		return codec.readValue(run.getState(), DungeonProgressState.class);
	}

	private void save(DungeonRun run, DungeonProgressState state) {
		run.setState(codec.writeValueAsString(state));
		run.setActiveBattleId(state.getActiveBattle());
		run.setActive(state.getCleared() < catalog.definition(state.getDungeon()).stages().size()
				&& state.getMembers().values().stream().anyMatch(member -> !member.isExited()));
	}

}

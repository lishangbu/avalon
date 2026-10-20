package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.application.model.*;
import io.github.lishangbu.avalon.battle.domain.*;
import io.github.lishangbu.avalon.battle.persistence.entity.*;
import io.github.lishangbu.avalon.battle.persistence.repository.*;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.model.CharacterBattleParticipant;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.GatheringCommandService;
import io.github.lishangbu.avalon.world.domain.LifeClock;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 双方角色锁先于共享战局锁；状态、资源、占用和回执在同一事务提交。 */
@Component
@RequiredArgsConstructor
public class PvpCommandService {

	private final CharacterCommandService characters;

	private final CharacterQueryService queries;

	private final GatheringCommandService gathering;

	private final WorldCatalog world;

	private final PartyFactory party;

	private final PvpBattleRepository battles;

	private final PvpCommandReceiptRepository receipts;

	private final CombatVitalsRepository vitals;

	private final CombatVitalsStore vitalStore;

	private final InventoryCommandService inventory;

	private final BattleViews battleViews;

	private final PvpViews views;

	private final PvpRoundCoordinator rounds;

	private final LifeClock clock;

	private final PvpProtectionService protection;

	private final ObjectMapper json;

	private final SecureRandom entropy = new SecureRandom();

	@Transactional
	public PvpView start(long owner, long opponent, UUID request) {
		if (request == null)
			throw new BusinessException(400, "缺少请求号");
		var pair = characters.lockBattlePair(owner, opponent);
		var self = pair.getFirst();
		var prior = battles.request(self.character(), request.toString());
		if (prior != null) {
			if (prior.getDefenderId() != opponent)
				throw new BusinessException(409, "请求号已用于另一对手");
			return view(owner, self.character(), prior);
		}
		if (!self.location().equals(pair.getLast().location()) || !world.provides(self.location(), "PVP"))
			throw new BusinessException(409, "当前地点不能攻击此玩家");
		protection.attack(self.character(), opponent);
		var battle = new BattleState();
		battle.setPvp(true);
		battle.setEncounter("PVP");
		battle.setSeed(entropy.nextLong());
		for (var participant : pair) {
			characters.requireOutsideDungeon(participant.owner());
			if (queries.lobby(participant.owner()) != null)
				throw new BusinessException(409, "对战成员正在副本组队");
			gathering.interruptForBattle(participant.owner());
			characters.requireNoLifeActivity(participant.owner());
			var units = party.create(participant.owner(), false);
			for (var unit : units) {
				unit.setEnemy(participant.character() != self.character());
				var saved = vitals.get(Long.parseLong(unit.getId()));
				if (saved != null) {
					unit.setHp(Math.min(unit.getHp(), saved.getHp()));
					unit.setMp(Math.min(unit.getMp(), saved.getMp()));
				}
			}
			if (units.stream().noneMatch(unit -> unit.active() && unit.alive()))
				throw new BusinessException(409, "对战队伍需要先恢复出战单位");
			battle.getUnits().addAll(units);
		}
		var state = rounds.open(battle, clock.now());
		var record = new PvpBattle();
		record.setAttackerId(self.character());
		record.setDefenderId(opponent);
		record.setRequestId(request.toString());
		record.setActive(true);
		record.setState(json.writeValueAsString(state));
		battles.saveAndFlush(record);
		pair.forEach(participant -> characters.setBattle(participant.owner(), record.getId(), false));
		return views.view(owner, self.character(), record.getId(), state);
	}

	@Transactional
	public PvpView turn(long owner, long id, UUID request, int round, int revision, List<BattleStateOrder> orders,
			Map<String, Integer> replacements, boolean surrender) {
		if (request == null || orders == null || replacements == null || round < 1 || revision < 0)
			throw new BusinessException(400, "对战指令格式无效");
		long character = queries.requireId(owner);
		var pair = lock(owner, character, id);
		var record = battles.lock(id);
		String payload = json
			.writeValueAsString(new BattleTurnPayload(request, revision, round, orders, surrender, replacements));
		var prior = receipts.request(id, character, request.toString());
		if (prior != null) {
			if (!prior.getPayload().equals(payload))
				throw new BusinessException(409, "同一请求号不能提交不同指令");
			return json.readValue(prior.getResponse(), PvpView.class);
		}
		var state = json.readValue(record.getState(), PvpRoundState.class);
		if (!record.isActive() || state.getBattle().getRound() != round || state.getBattle().getRevision() != revision)
			throw new BusinessException(409, "对战回合已变化，请刷新");
		var resources = resources(pair);
		if (surrender)
			rounds.surrender(state, Long.toString(character));
		else {
			rounds.submit(state, Long.toString(character), round, new DungeonRoundStateSubmission(orders, replacements),
					clock.now(), resources);
			var result = rounds.settle(state, clock.now(), resources);
			if (result.round() != null)
				for (var change : result.round().changes()) {
					if (!change.kind().equals("CONSUME"))
						throw new IllegalStateException("PVP不允许捕捉资产变更");
					var actor = state.getBattle()
						.getUnits()
						.stream()
						.filter(unit -> unit.getId().equals(change.owner()))
						.findFirst()
						.orElseThrow();
					resources.take(actor, change.reference());
				}
		}
		persist(record, state, pair);
		var response = views.view(owner, character, id, state);
		var receipt = new PvpCommandReceipt();
		receipt.setBattleId(id);
		receipt.setCharacterId(character);
		receipt.setRequestId(request.toString());
		receipt.setPayload(payload);
		receipt.setResponse(json.writeValueAsString(response));
		receipts.save(receipt);
		return response;
	}

	/** 重连或定时轮询推进过期窗口，不依赖在线连接或进程内计时器。 */
	@Transactional
	public PvpView refresh(long owner, long id) {
		long character = queries.requireId(owner);
		var pair = lock(owner, character, id);
		var record = battles.lock(id);
		var state = json.readValue(record.getState(), PvpRoundState.class);
		if (record.isActive()) {
			var resources = resources(pair);
			var result = rounds.settle(state, clock.now(), resources);
			if (result.round() != null)
				for (var change : result.round().changes()) {
					if (!change.kind().equals("CONSUME"))
						throw new IllegalStateException("PVP不允许捕捉资产变更");
					var actor = state.getBattle()
						.getUnits()
						.stream()
						.filter(unit -> unit.getId().equals(change.owner()))
						.findFirst()
						.orElseThrow();
					resources.take(actor, change.reference());
				}
			if (result.changed())
				persist(record, state, pair);
		}
		return views.view(owner, character, id, state);
	}

	private List<CharacterBattleParticipant> lock(long owner, long character, long id) {
		var participants = battles.participants(id);
		if (participants == null || participants.attacker() != character && participants.defender() != character)
			throw new BusinessException(404, "对战不存在");
		return characters.lockBattlePair(owner,
				participants.attacker() == character ? participants.defender() : participants.attacker());
	}

	private BattleEngineResources resources(List<CharacterBattleParticipant> pair) {
		return new BattleEngineResources() {
			private long owner(BattleStateUnit actor) {
				String hero = actor.isHero() ? actor.getId() : actor.getOwnerHero();
				return pair.stream()
					.filter(member -> Long.toString(member.character()).equals(hero))
					.findFirst()
					.orElseThrow()
					.owner();
			}

			public int count(BattleStateUnit actor, String code) {
				return battleViews.supplies(owner(actor)).getOrDefault(code, 0);
			}

			public boolean room(BattleStateUnit target) {
				return false;
			}

			public void take(BattleStateUnit actor, String code) {
				inventory.take(owner(actor), code, 1);
			}

			public void capture(BattleStateUnit target) {
				throw new IllegalStateException("PVP不能捕捉");
			}
		};
	}

	private void persist(PvpBattle record, PvpRoundState state, List<CharacterBattleParticipant> pair) {
		boolean active = state.getBattle().getStatus().equals("ACTIVE");
		for (var member : pair) {
			boolean enemy = member.character() == record.getDefenderId();
			for (var unit : state.getBattle().getUnits())
				if (unit.isEnemy() == enemy)
					vitalStore.saveVitals(member.character(), Long.parseLong(unit.getId()), unit.getHp(), unit.getMp());
			if (!active) {
				String status = state.getBattle().getStatus();
				characters.setBattle(member.owner(), null,
						status.equals("DRAW") || enemy && status.equals("WON") || !enemy && status.equals("LOST"));
			}
		}
		record.setActive(active);
		record.setState(json.writeValueAsString(state));
	}

	private PvpView view(long owner, long character, PvpBattle record) {
		return views.view(owner, character, record.getId(), json.readValue(record.getState(), PvpRoundState.class));
	}

}

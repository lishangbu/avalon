package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.domain.BattleEngineResources;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundCoordinator;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundState;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundStateSubmission;
import io.github.lishangbu.avalon.battle.persistence.entity.DungeonBattle;
import io.github.lishangbu.avalon.battle.persistence.entity.DungeonSubmission;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonBattleMemberStore;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonBattleRecordRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonSubmissionRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 多人战局持久化命令，仅供副本进度用例调用，不是开放开本接口。 调用方负责匹配、等级、编队与次数，并在本事务内提供已锁定入本物资的资源适配器。
 * 涉及库存时必须先锁副本，再按固定顺序锁成员角色，避免不同提交者反向取锁。
 */
@Component
@RequiredArgsConstructor
public class DungeonBattleCommandService {

	private final DungeonBattleMemberStore dungeonBattleMemberStore;

	private final DungeonBattleRecordRepository dungeonBattleRecordRepository;

	private final DungeonSubmissionRepository dungeonSubmissionRepository;

	private final DungeonRoundCoordinator rounds;

	private final ObjectMapper codec;

	/** 创建必须参与正式开本／开下一场事务，不能独立提交而绕过进度及角色门禁。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public long create(BattleState battle, Instant now) {
		var state = rounds.open(battle, now);
		var entity = new DungeonBattle();

		save(entity, state);
		dungeonBattleRecordRepository.saveAndFlush(entity);
		for (String character : state.getMembers().keySet())
			dungeonBattleMemberStore.addMember(entity.getId(), Long.parseLong(character));
		return entity.getId();
	}

	/** 先查已持久化凭证，完成后的旧提交重试也不会重新扣物品或推进回合。 */
	@Transactional
	public void submit(long id, long character, int round, DungeonRoundStateSubmission input, Instant now,
			BattleEngineResources resources) {
		var entity = require(id);
		requireMember(id, character);
		var previous = dungeonSubmissionRepository.submission(id, character, round);
		if (previous != null) {
			if (!codec.readValue(previous.getPayload(), DungeonRoundStateSubmission.class).equals(input))
				throw new BusinessException(409, "本轮已提交不同指令");
			return;
		}
		var state = codec.readValue(entity.getState(), DungeonRoundState.class);
		rounds.submit(state, Long.toString(character), round, input, now, resources);
		var receipt = new DungeonSubmission();

		receipt.setBattleId(id);
		receipt.setCharacterId(character);
		receipt.setRound(round);
		receipt.setPayload(codec.writeValueAsString(input));
		dungeonSubmissionRepository.save(receipt);
		rounds.settle(state, now, resources);
		save(entity, state);
		dungeonBattleRecordRepository.flush();
	}

	/** 候选扫描可能过期；拿到行锁后必须复核数据库中的当前截止时间。 */
	@Transactional
	public boolean expire(long id, Instant now, BattleEngineResources resources) {
		var entity = require(id);
		if (!entity.isActive() || entity.getDeadline().isAfter(now))
			return false;
		var state = codec.readValue(entity.getState(), DungeonRoundState.class);
		boolean changed = rounds.settle(state, now, resources);
		if (changed)
			save(entity, state);
		return changed;
	}

	@Transactional
	public void leave(long id, long character, Instant now, BattleEngineResources resources) {
		var entity = require(id);
		requireMember(id, character);
		if (!entity.isActive())
			return;
		var state = codec.readValue(entity.getState(), DungeonRoundState.class);
		rounds.leave(state, Long.toString(character));
		rounds.settle(state, now, resources);
		save(entity, state);
	}

	private DungeonBattle require(long id) {
		var entity = dungeonBattleRecordRepository.lock(id);
		if (entity == null)
			throw new BusinessException(404, "副本战局不存在");
		return entity;
	}

	private void requireMember(long id, long character) {
		if (!dungeonBattleMemberStore.member(id, character))
			throw new BusinessException(404, "副本战局不存在");
	}

	private void save(DungeonBattle entity, DungeonRoundState state) {
		entity.setActive(state.getBattle().getStatus().equals("ACTIVE"));
		entity.setDeadline(state.getDeadline());
		entity.setState(codec.writeValueAsString(state));
	}

}

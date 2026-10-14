package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.application.model.DungeonSubmitPayload;
import java.util.UUID;

import io.github.lishangbu.avalon.battle.domain.BattleEngineResources;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundCoordinator;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundState;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundResult;
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
import io.github.lishangbu.avalon.battle.replay.BattleReplayJournal;
import io.github.lishangbu.avalon.battle.replay.BattleReplayCodec;
import io.github.lishangbu.avalon.battle.domain.BattleRoundResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 多人战局持久化命令，仅供副本进度用例调用，不是开放开本接口。 调用方负责匹配、等级、编队与次数，并在本事务内提供已锁定入本物资的资源适配器。
 * 涉及库存时必须先锁副本，再按固定顺序锁成员角色，避免不同提交者反向取锁。
 */
@Component
@RequiredArgsConstructor
public class DungeonBattleCommandService {

	/** 文本记录只在事务提交后落盘，不参与数据库资产写入。 */
	private final BattleReplayJournal journal;

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
		journal.start("DUNGEON", entity.getId(), state.getBattle());
		for (String character : state.getMembers().keySet())
			dungeonBattleMemberStore.addMember(entity.getId(), Long.parseLong(character));
		return entity.getId();
	}

	/** 先查已持久化凭证，完成后的旧提交重试也不会重新扣物品或推进回合。 */
	@Transactional
	public void submit(long id, long character, UUID requestId, int revision, int round,
			DungeonRoundStateSubmission input, Instant now, BattleEngineResources resources) {
		var entity = require(id);
		requireMember(id, character);
		if (requestId == null || revision < 0 || round < 1 || input == null)
			throw new BusinessException(400, "副本回合请求格式无效");
		var payload = new DungeonSubmitPayload(requestId, revision, round, input.orders(), input.replacements());
		String canonical = BattleReplayCodec.write(payload);
		var previous = dungeonSubmissionRepository.request(id, character, requestId.toString());
		if (previous != null) {
			if (!previous.getPayload().equals(canonical))
				throw new BusinessException(409, "同一请求号不能提交不同指令");
			return;
		}
		if (dungeonSubmissionRepository.submission(id, character, round) != null)
			throw new BusinessException(409, "本轮已由其他请求锁定");
		var state = codec.readValue(entity.getState(), DungeonRoundState.class);
		if (state.getBattle().getRevision() != revision)
			throw new BusinessException(409, "副本战局修订已变化，请刷新");
		rounds.submit(state, Long.toString(character), round, input, now, resources);
		var receipt = new DungeonSubmission();

		receipt.setBattleId(id);
		receipt.setCharacterId(character);
		receipt.setRound(round);
		receipt.setRequestId(requestId.toString());
		receipt.setPayload(canonical);
		dungeonSubmissionRepository.save(receipt);
		settle(id, state, now, resources);
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
		boolean changed = settle(id, state, now, resources);
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
		settle(id, state, now, resources);
		save(entity, state);
	}

	/** 先冻结规则证据再应用资产，成员退出引起的终局也在提交后关闭文件。 */
	private boolean settle(long id, DungeonRoundState state, Instant now, BattleEngineResources resources) {
		// 全员倒下且未补位可能直接终局，仍须保存宿主结算前状态供无动作变化记录使用。
		var before = BattleReplayCodec.read(BattleReplayCodec.write(state.getBattle()), BattleState.class);
		var result = rounds.settle(state, now, resources);
		if (!result.changed())
			return false;
		if (result.round() != null) {
			var evidence = BattleReplayCodec.read(BattleReplayCodec.write(result.round()), BattleRoundResult.class);
			apply(result, resources);
			journal.round("DUNGEON", id, "round-" + evidence.before().getRound(), evidence.before(), evidence,
					"ACTIVE");
			journal.state("DUNGEON", id, "DUNGEON_MEMBER_SETTLEMENT", evidence.state(), state.getBattle());
			if (!state.getBattle().getStatus().equals("ACTIVE"))
				journal.finish("DUNGEON", id, state.getBattle().getStatus());
		}
		else {
			journal.state("DUNGEON", id, "DUNGEON_NO_ACTION_SETTLEMENT", before, state.getBattle());
			if (!state.getBattle().getStatus().equals("ACTIVE"))
				journal.finish("DUNGEON", id, state.getBattle().getStatus());
		}
		return true;
	}

	/** 纯规则成功后才提交真实物资；异常由外层事务同时回滚战局、回执与物资。 */
	private void apply(DungeonRoundResult result, BattleEngineResources resources) {
		if (result.round() == null)
			return;
		for (var change : result.round().changes()) {
			if (!change.kind().equals("CONSUME"))
				throw new IllegalStateException("副本不允许此资产结果：" + change.kind());
			var actor = result.round()
				.state()
				.getUnits()
				.stream()
				.filter(unit -> unit.getId().equals(change.owner()))
				.findFirst()
				.orElseThrow();
			for (long count = 0; count < change.amount(); count++)
				resources.take(actor, change.reference());
		}
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

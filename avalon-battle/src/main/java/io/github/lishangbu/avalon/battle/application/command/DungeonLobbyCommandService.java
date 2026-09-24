package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.domain.DungeonLobbyState;
import io.github.lishangbu.avalon.battle.persistence.entity.DungeonLobby;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonLobbyRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 三人确认的持久化事务。等待组满时可以正常冒险，确认后只保留有限时间的位置。 房间锁先于角色锁；全员准备后沿用正式开本事务，任何失败均不单独扣次数。
 */
@Component
@RequiredArgsConstructor
public class DungeonLobbyCommandService {

	private final DungeonLobbyRepository lobbies;

	private final ObjectMapper codec;

	private final CharacterCommandService characters;

	private final CharacterQueryService queries;

	private final CharacterFactQueryService facts;

	private final DungeonRunCommandService runs;

	private final DungeonCatalog catalog;

	/** 主动匹配只分配等待位置，不确认、不准备、不扣次，也不结束个人战斗。 */
	@Transactional
	public long match(long owner) {
		lobbies.lockMatching();
		Long current = queries.lobby(owner);
		if (current != null) {
			var state = decode(require(current));
			if (!state.isClosed() && state.isAutomatic() && state.getMembers().containsKey(owner))
				return current;
			throw new BusinessException(409, "请先离开当前队伍再匹配");
		}
		var waiting = lobbies.firstMatching(catalog.definition().players());
		if (waiting != null) {
			join(waiting.getId(), owner);
			return waiting.getId();
		}
		long id = create(owner, UUID.randomUUID().toString());
		var row = require(id);
		var state = decode(row);
		state.setAutomatic(true);
		save(row, state);
		return id;
	}

	/** 候补者主动申请才创建确认房间；每个账号仍只能保留一个组队位置。 */
	@Transactional
	public long replacement(long run, long owner, String request) {
		if (request == null || !request.matches("[a-zA-Z0-9-]{8,64}"))
			throw new BusinessException(400, "请求号无效");
		var replay = lobbies.request(queries.requireId(owner), request);
		if (replay != null) {
			if (!Objects.equals(decode(replay).getTargetRun(), run))
				throw new BusinessException(409, "请求号已用于其他队伍");
			return replay.getId();
		}
		// 进度锁必须先于角色锁，与实际补员、战间准备使用相同顺序。
		runs.requireRecruiting(run, owner);
		characters.lock(owner);
		var prior = lobbies.request(queries.requireId(owner), request);
		if (prior != null) {
			if (!Objects.equals(decode(prior).getTargetRun(), run))
				throw new BusinessException(409, "请求号已用于其他队伍");
			return prior.getId();
		}
		eligible(owner);
		var state = new DungeonLobbyState();
		state.setLeader(owner);
		state.setTargetRun(run);
		var member = new DungeonLobbyState.Member();
		member.setDeadline(Instant.now().plusSeconds(catalog.definition().confirmSeconds()));
		state.getMembers().put(owner, member);
		var row = new DungeonLobby();

		row.setLeaderId(queries.requireId(owner));
		row.setRequestId(request);
		save(row, state);
		lobbies.saveAndFlush(row);
		characters.reserveLobby(owner, row.getId());
		return row.getId();
	}

	@Transactional
	public long create(long owner, String request) {
		if (request == null || !request.matches("[a-zA-Z0-9-]{8,64}"))
			throw new BusinessException(400, "请求号无效");
		characters.lock(owner);
		var prior = lobbies.request(queries.requireId(owner), request);
		if (prior != null) {
			if (decode(prior).getTargetRun() != null)
				throw new BusinessException(409, "请求号已用于补员确认");
			return prior.getId();
		}
		eligible(owner);
		var state = new DungeonLobbyState();
		state.setLeader(owner);
		state.getMembers().put(owner, new DungeonLobbyState.Member());
		var row = new DungeonLobby();

		row.setLeaderId(queries.requireId(owner));
		row.setRequestId(request);
		save(row, state);
		lobbies.saveAndFlush(row);
		characters.reserveLobby(owner, row.getId());
		return row.getId();
	}

	/** 招募加入只占待确认位置；满三人后才开始六十秒确认，不能替玩家默认接受。 */
	@Transactional
	public void join(long lobby, long owner) {
		var row = require(lobby);
		var state = decode(row);
		requireOpen(state);
		if (state.getMembers().containsKey(owner))
			return;
		if (state.getTargetRun() != null)
			throw new BusinessException(409, "补员确认仅属于申请者本人");
		if (state.getMembers().size() >= catalog.definition().players())
			throw new BusinessException(409, "队伍已满");
		eligible(owner);
		characters.reserveLobby(owner, lobby);
		state.getMembers().put(owner, new DungeonLobbyState.Member());
		if (state.getMembers().size() == catalog.definition().players()) {
			Instant until = Instant.now().plusSeconds(catalog.definition().confirmSeconds());
			state.getMembers()
				.values()
				.stream()
				.filter(member -> member.getDeadline() == null)
				.forEach(member -> member.setDeadline(until));
		}
		save(row, state);
	}

	@Transactional
	public void confirm(long lobby, long owner) {
		var row = require(lobby);
		var state = decode(row);
		var member = member(state, owner);
		if (member.getDeadline() == null)
			throw new BusinessException(409, "队伍尚未组满");
		requireInTime(member);
		if (member.isConfirmed())
			return;
		if (state.getTargetRun() != null)
			runs.requireRecruiting(state.getTargetRun(), owner);
		eligible(owner);
		Instant until = Instant.now().plusSeconds(catalog.definition().holdSeconds());
		characters.confirmLobby(owner, lobby, until);
		member.setConfirmed(true);
		member.setDeadline(until);
		save(row, state);
	}

	@Transactional
	public void ready(long lobby, long owner) {
		var row = require(lobby);
		var state = decode(row);
		var member = member(state, owner);
		requireInTime(member);
		if (!member.isConfirmed())
			throw new BusinessException(409, "请先确认入队");
		characters.requireOutsideBattle(owner);
		member.setReady(true);
		member.setPreparedRevision(queries.adventure(owner).preparationRevision());
		save(row, state);
	}

	/** 客户端只提供房间号；队员名单、发起者和防重键均取服务端存档。 */
	@Transactional
	public long launch(long lobby, long owner) {
		var row = require(lobby);
		var state = decode(row);
		if (!state.getMembers().containsKey(owner))
			throw new BusinessException(404, "队伍不存在");
		if (state.getRun() != null)
			return state.getRun();
		requireOpen(state);
		if (state.getTargetRun() != null) {
			runs.requireRecruiting(state.getTargetRun(), owner);
			characters.lock(owner);
			var candidate = state.getMembers().get(owner);
			requireInTime(candidate);
			if (!candidate.isConfirmed() || !candidate.isReady()
					|| candidate.getPreparedRevision() != queries.adventure(owner).preparationRevision())
				throw new BusinessException(409, "请重新核验并准备");
			runs.join(state.getTargetRun(), owner);
			state.setRun(state.getTargetRun());
			state.setClosed(true);
			characters.releaseLobby(owner, lobby);
			save(row, state);
			return state.getRun();
		}
		if (state.getMembers().size() != catalog.definition().players())
			throw new BusinessException(409, "需要三名成员");
		if (!state.getMembers().containsKey(state.getLeader()))
			throw new BusinessException(409, "请先由一名成员接任发起者");
		var owners = state.getMembers().keySet().stream().sorted().toList();
		owners.forEach(characters::lock);
		for (long candidate : owners) {
			var member = state.getMembers().get(candidate);
			requireInTime(member);
			if (!member.isConfirmed() || !member.isReady()
					|| member.getPreparedRevision() != queries.adventure(candidate).preparationRevision())
				throw new BusinessException(409, "需要全员重新核验并准备");
			eligible(candidate);
			characters.requireOutsideBattle(candidate);
		}
		long run = runs.create(state.getLeader(), owners, "LOBBY-" + lobby);
		state.setRun(run);
		state.setClosed(true);
		owners.forEach(candidate -> characters.releaseLobby(candidate, lobby));
		save(row, state);
		return run;
	}

	/** 拒绝或超时不影响已有个人战斗；普通队员离开后其余成员可继续补人。 */
	@Transactional
	public void leave(long lobby, long owner) {
		var row = require(lobby);
		var state = decode(row);
		requireOpen(state);
		if (!state.getMembers().containsKey(owner))
			return;
		remove(row.getId(), state, owner);
		save(row, state);
	}

	/** 调度器驱动清理，不依赖刷新页面，也不追补任何战斗行动。 */
	@Transactional
	public void expire(long lobby) {
		var row = require(lobby);
		var state = decode(row);
		if (state.isClosed())
			return;
		var expired = state.getMembers()
			.entrySet()
			.stream()
			.filter(entry -> entry.getValue().getDeadline() != null
					&& !Instant.now().isBefore(entry.getValue().getDeadline()))
			.map(Map.Entry::getKey)
			.toList();
		for (long owner : expired) {
			if (state.isClosed())
				break;
			remove(lobby, state, owner);
		}
		save(row, state);
	}

	private void remove(long lobby, DungeonLobbyState state, long owner) {
		characters.releaseLobby(owner, lobby);
		state.getMembers().remove(owner);
		// 留下其余成员继续补人，但不擅自指定新的扣次者；0 仅是内部“暂无发起者”状态。
		if (state.getLeader() == owner)
			state.setLeader(0);
		if (state.getMembers().isEmpty())
			state.setClosed(true);
	}

	/** 原发起者拒绝或超时后，现有成员明确接任才承担正式开本次数。 */
	@Transactional
	public void lead(long lobby, long owner) {
		var row = require(lobby);
		var state = decode(row);
		member(state, owner);
		if (state.getTargetRun() != null)
			throw new BusinessException(409, "补员不改变原副本发起者");
		if (state.getLeader() == owner)
			return;
		if (state.getLeader() != 0)
			throw new BusinessException(409, "队伍已有发起者");
		eligible(owner);
		state.setLeader(owner);
		save(row, state);
	}

	private void eligible(long owner) {
		var context = characters.lock(owner);
		if (context.level() < catalog.definition().level() || queries.adventure(owner).dungeon() != null
				|| !facts.codes(owner).contains("QUEST:" + catalog.definition().requires()))
			throw new BusinessException(409, "尚未满足副本入队条件");
	}

	private DungeonLobbyState.Member member(DungeonLobbyState state, long owner) {
		requireOpen(state);
		var member = state.getMembers().get(owner);
		if (member == null)
			throw new BusinessException(404, "队伍不存在");
		return member;
	}

	private void requireInTime(DungeonLobbyState.Member member) {
		if (member.getDeadline() == null || !Instant.now().isBefore(member.getDeadline()))
			throw new BusinessException(409, "入队确认或位置保留已到期");
	}

	private void requireOpen(DungeonLobbyState state) {
		if (state.isClosed())
			throw new BusinessException(409, "组队已经结束");
	}

	private DungeonLobby require(long lobby) {
		var row = lobbies.lock(lobby);
		if (row == null)
			throw new BusinessException(404, "队伍不存在");
		return row;
	}

	private DungeonLobbyState decode(DungeonLobby row) {
		return codec.readValue(row.getState(), DungeonLobbyState.class);
	}

	private void save(DungeonLobby row, DungeonLobbyState state) {
		row.setState(codec.writeValueAsString(state));
		row.setOpen(!state.isClosed());
		row.setDeadline(state.isClosed() ? null
				: state.getMembers()
					.values()
					.stream()
					.map(DungeonLobbyState.Member::getDeadline)
					.filter(Objects::nonNull)
					.min(Comparator.naturalOrder())
					.orElse(null));
	}

}

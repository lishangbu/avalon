package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** 多人手动提交与到期结算的领域规则，不自行创建线程，也不依赖在线连接维持战局。 */
@Component
@RequiredArgsConstructor
public class DungeonRoundCoordinator {

	private final BattleEngine engine;

	private final DungeonCatalog catalog;

	private final ObjectMapper json;

	public DungeonRoundState open(BattleState battle, Instant now) {
		if (battle.getDungeonStage() == null || !battle.getStatus().equals("ACTIVE"))
			throw new IllegalArgumentException("只能开启进行中的副本战局");
		var state = new DungeonRoundState();
		state.setBattle(battle);
		battle.getUnits().stream().filter(unit -> !unit.isEnemy() && unit.isHero()).forEach(unit -> {
			if (state.getMembers().put(unit.getId(), new DungeonRoundStateMember()) != null)
				throw new IllegalArgumentException("副本成员编号重复");
		});
		if (state.getMembers().size() != catalog.definition().players())
			throw new IllegalArgumentException("首次进入战斗需要完整队伍");
		for (var unit : battle.getUnits()) {
			if (!unit.isEnemy() && !unit.isHero() && !state.getMembers().containsKey(unit.getOwnerHero()))
				throw new IllegalArgumentException("幻兽必须归属本场成员");
		}
		engine.planEnemies(battle);
		state.setDeadline(now.plusSeconds(catalog.definition().roundSeconds()));
		return state;
	}

	/** 同轮完全相同的提交可重试；一旦锁定不能撤回或更换选招。 */
	public void submit(DungeonRoundState state, String hero, int round, DungeonRoundStateSubmission input, Instant now,
			BattleEngineResources resources) {
		var member = state.getMembers().get(hero);
		if (member == null || member.isRemoved() || member.isLeaving())
			fail("你不在本场可行动成员中");
		if (!state.getBattle().getStatus().equals("ACTIVE") || round != state.getBattle().getRound())
			fail("回合已发生变化，请刷新战局");
		if (input == null)
			fail("请提交手动行动");
		var previous = state.getSubmissions().get(hero);
		if (previous != null) {
			if (!previous.equals(input))
				fail("本轮行动已经锁定");
			return;
		}
		if (!now.isBefore(state.getDeadline()))
			fail("本轮提交时间已结束");
		// 免费补位的校验也会调整位置，必须在副本上完成，避免提前影响其他成员选招。
		engine.validateMember(copy(state.getBattle()), hero, input.orders(), input.replacements(), resources);
		state.getSubmissions().put(hero, input);
		if (!input.orders().isEmpty())
			member.setParticipated(true);
	}

	/** 只有全部提交或到期才结算一次；新截止时间从本次处理时刻开始，重启后不连续自动追赶回合。 */
	public boolean settle(DungeonRoundState state, Instant now, BattleEngineResources resources) {
		var battle = state.getBattle();
		if (!battle.getStatus().equals("ACTIVE"))
			return false;
		boolean complete = state.getMembers()
			.entrySet()
			.stream()
			.filter(entry -> !entry.getValue().isRemoved() && !entry.getValue().isLeaving())
			.allMatch(entry -> state.getSubmissions().containsKey(entry.getKey()));
		if (!complete && now.isBefore(state.getDeadline()))
			return false;
		for (var entry : state.getMembers().entrySet()) {
			var member = entry.getValue();
			if (member.isRemoved())
				continue;
			member
				.setMissedRounds(state.getSubmissions().containsKey(entry.getKey()) ? 0 : member.getMissedRounds() + 1);
		}
		List<BattleStateOrder> orders = new ArrayList<>();
		Map<String, Integer> replacements = new LinkedHashMap<>();
		for (var entry : state.getMembers().entrySet()) {
			if (entry.getValue().isRemoved())
				continue;
			var input = state.getSubmissions().get(entry.getKey());
			if (input != null) {
				orders.addAll(input.orders());
				replacements.putAll(input.replacements());
			}
			else {
				battle.getUnits()
					.stream()
					.filter(unit -> owned(unit, entry.getKey()) && unit.active() && unit.alive())
					.forEach(unit -> orders.add(new BattleStateOrder(unit.getId(), "NO_ACTION", null)));
			}
		}
		// 上轮场上全倒时，只给这一轮手动补位机会；替补存在不能让战局无限悬停。
		if (battle.getUnits().stream().noneMatch(unit -> !unit.isEnemy() && unit.active() && unit.alive())
				&& replacements.isEmpty()) {
			battle.setStatus("LOST");
			battle.getLog().add("场上成员全部倒下且本轮无人补位，本场挑战失败。");
		}
		else {
			engine.resolve(battle, orders, false, resources, replacements);
		}
		// 本轮伤害和已锁定指令执行完毕，才移除缺席或主动退出成员，且保留实际资源。
		for (var entry : state.getMembers().entrySet()) {
			var member = entry.getValue();
			if (member.isRemoved())
				continue;
			if (member.isLeaving() || member.getMissedRounds() >= catalog.definition().absenceRounds()) {
				member.setRemoved(true);
				state.getWithdrawn()
					.put(entry.getKey(),
							battle.getUnits().stream().filter(unit -> owned(unit, entry.getKey())).toList());
				battle.getUnits().removeIf(unit -> owned(unit, entry.getKey()));
				battle.getLog().add("成员 " + entry.getKey() + " 已在本轮结算后退出本场战斗。");
			}
		}
		if (state.getMembers().values().stream().allMatch(DungeonRoundStateMember::isRemoved)
				|| (battle.getStatus().equals("ACTIVE")
						&& battle.getUnits().stream().noneMatch(unit -> !unit.isEnemy() && unit.alive())))
			battle.setStatus("LOST");
		if (!battle.getStatus().equals("ACTIVE"))
			battle.setTelegraph(null);
		else
			engine.planEnemies(battle);
		state.getSubmissions().clear();
		state.setDeadline(now.plusSeconds(catalog.definition().roundSeconds()));
		return true;
	}

	public boolean eligible(DungeonRoundState state, String hero) {
		var member = state.getMembers().get(hero);
		return member != null && member.isParticipated() && !member.isRemoved() && !member.isLeaving();
	}

	/** 主动退出不撤销已锁定指令，最后资源在当前回合结束后归档。 */
	public void leave(DungeonRoundState state, String hero) {
		var member = state.getMembers().get(hero);
		if (member == null)
			fail("你不是本场成员");
		member.setLeaving(true);
	}

	private boolean owned(BattleStateUnit unit, String hero) {
		return !unit.isEnemy() && Objects.equals(hero, unit.isHero() ? unit.getId() : unit.getOwnerHero());
	}

	private BattleState copy(BattleState state) {
		try {
			return json.readValue(json.writeValueAsBytes(state), BattleState.class);
		}
		catch (JacksonException exception) {
			throw new IllegalStateException("无法复制战局进行提交校验", exception);

		}
	}

	private void fail(String message) {
		throw new BusinessException(409, message);
	}

}

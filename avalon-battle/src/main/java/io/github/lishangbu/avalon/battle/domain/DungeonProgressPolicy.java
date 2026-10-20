package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 副本战间状态机。入口资格、锁定编队、扣次数由应用层负责；本类不发奖励、不恢复真实资源。 休整校验通过后，应用层须在同一事务恢复真实资源；失败回到休整点本身不等于已休整。
 */
@Component
@RequiredArgsConstructor
public class DungeonProgressPolicy {

	private final DungeonCatalog catalog;

	public DungeonProgressState create(String leader, List<String> members) {
		return create(catalog.definition().id(), leader, members);
	}

	public DungeonProgressState create(String dungeon, String leader, List<String> members) {
		if (members == null || members.size() != catalog.definition(dungeon).players()
				|| members.stream().anyMatch(member -> member == null || member.isBlank())
				|| new HashSet<>(members).size() != members.size() || !members.contains(leader))
			throw new BusinessException(409, "需要" + catalog.definition(dungeon).players() + "名不同成员且包含发起者");
		var state = new DungeonProgressState();
		state.setDungeon(catalog.definition(dungeon).id());
		state.setLeader(leader);
		members.forEach(member -> state.getMembers().put(member, new DungeonProgressStateMember()));
		return state;
	}

	/** 只补空缺，不能替换尚未退出的缺席者；中途加入者不继承已完成遭遇奖励。 */
	public void join(DungeonProgressState state, String character) {
		requireBetween(state);
		requireUnfinished(state);
		if (character == null || character.isBlank())
			throw new BusinessException(400, "成员编号无效");
		var previous = state.getMembers().get(character);
		if (previous != null) {
			if (previous.isExited())
				throw new BusinessException(409, "主动退出后不能返回同一副本进度");
			return;
		}
		if (state.getMembers().values().stream().filter(member -> !member.isExited()).count() >= catalog
			.definition(state.getDungeon())
			.players())
			throw new BusinessException(409, "副本队伍已满");
		// 补员从已解锁的最近休整点进入，仍需走到前线并手动准备。
		var member = new DungeonProgressStateMember();
		member.setLocation(nearestRest(state));
		state.getMembers().put(character, member);
	}

	public void move(DungeonProgressState state, String character, int location) {
		requireBetween(state);
		var member = member(state, character);
		if (location < 0 || location > state.getCleared() || Math.abs(location - member.getLocation()) > 1)
			throw new BusinessException(409, "只能沿已清理路线移动到相邻区域");
		if (location == member.getLocation())
			return;
		member.setLocation(location);
		member.setReady(false);
	}

	public void rest(DungeonProgressState state, String character) {
		requireBetween(state);
		var member = member(state, character);
		if (!catalog.definition(state.getDungeon()).restStops().contains(member.getLocation()))
			throw new BusinessException(409, "需要走到已解锁的休整点");
		member.setReady(false);
	}

	/** 允许带伤准备，但必须到达当前前线；属性、库存等校验由应用层持角色锁完成。 */
	public void ready(DungeonProgressState state, String character) {
		requireBetween(state);
		requireUnfinished(state);
		var member = member(state, character);
		if (member.getLocation() != state.getCleared())
			throw new BusinessException(409, "请先回到当前遭遇入口");
		member.setReady(true);
	}

	/** 换装、用药、休整或战间断线均取消本人准备；战斗中断线不撤销已锁定行动。 */
	public void invalidateReady(DungeonProgressState state, String character) {
		var member = member(state, character);
		if (state.getActiveBattle() == null)
			member.setReady(false);
	}

	/** 仅供战间退出或战局结算后的离场写回；战中请求应先交给回合协调器。 */
	public void leave(DungeonProgressState state, String character) {
		requireBetween(state);
		var member = state.getMembers().get(character);
		if (member == null)
			throw new BusinessException(404, "副本成员不存在");
		member.setExited(true);
		member.setReady(false);
	}

	/** 应用层先核验此方法，再创建战局并绑定；外层事务失败时两者一起回滚。 */
	public String nextStage(DungeonProgressState state) {
		requireBetween(state);
		requireUnfinished(state);
		var members = state.getMembers().values().stream().filter(member -> !member.isExited()).toList();
		if (members.size() != catalog.definition(state.getDungeon()).players()
				|| members.stream().anyMatch(member -> !member.isReady() || member.getLocation() != state.getCleared()))
			throw new BusinessException(409, "需要" + catalog.definition(state.getDungeon()).players() + "名成员到场并手动准备");
		return catalog.definition(state.getDungeon()).stages().get(state.getCleared()).id();
	}

	public void begin(DungeonProgressState state, long battle) {
		nextStage(state);
		if (battle <= 0 || state.getResolvedBattles().containsKey(battle))
			throw new BusinessException(409, "战局编号已使用或无效");
		state.setActiveBattle(battle);
		state.getMembers().values().forEach(member -> member.setReady(false));
	}

	/** 返回 false 表示已处理的同结果重试；只有 true 才允许上层执行首次结算。 */
	public boolean resolve(DungeonProgressState state, long battle, String outcome) {
		if (!List.of("WON", "LOST").contains(outcome))
			throw new BusinessException(409, "战局尚未结束");
		var previous = state.getResolvedBattles().get(battle);
		if (previous != null) {
			if (!previous.equals(outcome))
				throw new BusinessException(409, "已结算战局结果不一致");
			return false;
		}
		if (!Objects.equals(state.getActiveBattle(), battle))
			throw new BusinessException(409, "不是当前副本战局");
		state.getResolvedBattles().put(battle, outcome);
		state.setActiveBattle(null);
		if (outcome.equals("WON"))
			state.setCleared(state.getCleared() + 1);
		int location = outcome.equals("WON") ? state.getCleared() : nearestRest(state);
		state.getMembers().values().stream().filter(member -> !member.isExited()).forEach(member -> {
			member.setLocation(location);
			member.setReady(false);
		});
		return true;
	}

	private int nearestRest(DungeonProgressState state) {
		return catalog.definition(state.getDungeon())
			.restStops()
			.stream()
			.filter(stop -> stop <= state.getCleared())
			.mapToInt(Integer::intValue)
			.max()
			.orElseThrow();
	}

	private DungeonProgressStateMember member(DungeonProgressState state, String character) {
		var member = state.getMembers().get(character);
		if (member == null || member.isExited())
			throw new BusinessException(404, "不在此副本进度中");
		return member;
	}

	private void requireBetween(DungeonProgressState state) {
		if (!catalog.definition(state.getDungeon()).id().equals(state.getDungeon()))
			throw new BusinessException(409, "副本编号不匹配");
		if (state.getActiveBattle() != null)
			throw new BusinessException(409, "请先完成当前副本战斗");
	}

	private void requireUnfinished(DungeonProgressState state) {
		if (state.getCleared() >= catalog.definition(state.getDungeon()).stages().size())
			throw new BusinessException(409, "副本已经通关");
	}

}

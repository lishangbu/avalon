package io.github.lishangbu.avalon.battle.application.query;

import io.github.lishangbu.avalon.battle.application.model.DungeonProgressCurrent;
import io.github.lishangbu.avalon.battle.application.model.DungeonRunView;
import io.github.lishangbu.avalon.battle.application.model.DungeonRunViewMember;
import io.github.lishangbu.avalon.battle.application.model.DungeonRunViewNode;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.inventory.application.query.DungeonSupplyQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 读取当前进度不创建副本、不自动休整、不确认准备；按钮权限只是服务端规则的展示投影。 */
@Component
@RequiredArgsConstructor
public class DungeonProgressQueryService {

	private final DungeonRunQueryService runs;

	private final DungeonBattleQueryService battles;

	private final CharacterQueryService characters;

	private final DungeonSupplyQueryService supplies;

	private final DungeonCatalog catalog;

	@Transactional
	public DungeonProgressCurrent current(long owner) {
		Long run = characters.adventure(owner).dungeon();
		return new DungeonProgressCurrent(run == null ? null : view(run, owner), runs.remainingStarts(owner));
	}

	@Transactional
	public DungeonRunView view(long run, long owner) {
		var state = runs.restore(run, owner);
		String hero = characters.requireId(owner).toString();
		var self = state.getMembers().get(hero);
		if (self.isExited())
			throw new BusinessException(404, "不在此副本进度中");
		var definition = catalog.definition();
		boolean completed = state.getCleared() >= definition.stages().size();
		boolean between = state.getActiveBattle() == null && !completed;
		var members = state.getMembers()
			.entrySet()
			.stream()
			.filter(entry -> !entry.getValue().isExited())
			.map(entry -> {
				var member = entry.getValue();
				var character = characters.find(member.getOwner());
				return new DungeonRunViewMember(entry.getKey(), character.name(), character.profession(),
						character.level(), member.getLocation(), member.isReady());
			})
			.toList();
		var route = new ArrayList<DungeonRunViewNode>();
		route.add(new DungeonRunViewNode(0, "副本入口", true, definition.restStops().contains(0)));
		for (int index = 0; index < definition.stages().size(); index++)
			route.add(new DungeonRunViewNode(index + 1, definition.stages().get(index).name(),
					index < state.getCleared(), definition.restStops().contains(index + 1)));
		List<Integer> destinations = between ? IntStream.rangeClosed(0, state.getCleared())
			.filter(position -> Math.abs(position - self.getLocation()) == 1)
			.boxed()
			.toList() : List.of();
		boolean canBegin = between && members.size() == definition.players()
				&& members.stream().allMatch(member -> member.ready() && member.location() == state.getCleared());
		Long previous = battles.latestParticipated(state.getResolvedBattles().keySet(), Long.parseLong(hero));
		return new DungeonRunView(Long.toString(run), definition.name(), state.getLeader(), hero, state.getCleared(),
				completed, state.getActiveBattle() == null ? null : state.getActiveBattle().toString(),
				previous == null ? null : previous.toString(), canBegin,
				between && self.getLocation() == state.getCleared() && !self.isReady(),
				between && definition.restStops().contains(self.getLocation()), state.isRecruiting(),
				between && members.size() < definition.players(), destinations, List.copyOf(route), members,
				supplies.available(run, owner));
	}

}

package io.github.lishangbu.avalon.battle.application.model;

import io.github.lishangbu.avalon.battle.domain.BattleStateCodec;
import io.github.lishangbu.avalon.battle.domain.PvpRoundState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PvpViews {

	private final BattleViews views;

	public PvpView view(long owner, long character, long id, PvpRoundState round) {
		var state = BattleStateCodec.copy(round.getBattle());
		String heroId = Long.toString(character);
		var self = state.getUnits()
			.stream()
			.filter(unit -> unit.isHero() && unit.getId().equals(heroId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("对战投影角色不存在"));
		if (self.isEnemy()) {
			state.getUnits().forEach(unit -> unit.setEnemy(!unit.isEnemy()));
			state.setStatus(switch (state.getStatus()) {
				case "WON" -> "LOST";
				case "LOST" -> "WON";
				default -> state.getStatus();
			});
		}
		return new PvpView(views.view(owner, id, state), round.getSubmissions().containsKey(heroId),
				round.getSubmissions().keySet().stream().anyMatch(key -> !key.equals(heroId)), round.getDeadline());
	}

}

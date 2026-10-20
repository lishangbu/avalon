package io.github.lishangbu.avalon.battle.api.model;

import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PvpTurnRequest(UUID requestId, int round, int revision, List<BattleStateOrder> orders,
		Map<String, Integer> replacements, boolean surrender) {
}

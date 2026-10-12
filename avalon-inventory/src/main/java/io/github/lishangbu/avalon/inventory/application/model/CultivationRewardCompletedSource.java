package io.github.lishangbu.avalon.inventory.application.model;

import io.github.lishangbu.avalon.inventory.domain.CultivationRewardRulesKind;
import io.github.lishangbu.avalon.inventory.domain.CultivationRewardRulesStone;

/** seed来自服务器事件，stone在本次来源领取时确定；它不是背包内强化石兑换命令。 */
public record CultivationRewardCompletedSource(CultivationRewardRulesKind kind, String eventId, String route,
		CultivationRewardRulesStone stone, long seed) {
}

package io.github.lishangbu.avalon.battle.api.model;

import io.github.lishangbu.avalon.battle.application.model.BattleView;

/**
 * 无历史战局也返回稳定 JSON 外壳，避免 204 被浏览器查询缓存误判为缺失响应。
 */
public record BattleCurrent(BattleView battle) {
}

package io.github.lishangbu.avalon.battle.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** 可持久化的双方回合窗口；提交内容在结算前不向对方投影。 */
@Getter
@Setter
public class PvpRoundState {

	private BattleState battle;

	private Instant deadline;

	private Map<String, DungeonRoundStateSubmission> submissions = new LinkedHashMap<>();

}

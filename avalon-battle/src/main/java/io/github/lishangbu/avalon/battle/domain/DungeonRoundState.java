package io.github.lishangbu.avalon.battle.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** 可持久化的多人回合存档；读取、提交、到期处理必须由上层在同一副本锁下执行。 */
@Getter
@Setter
public class DungeonRoundState {

	private BattleState battle;

	private Instant deadline;

	private Map<String, DungeonRoundStateMember> members = new LinkedHashMap<>();

	private Map<String, DungeonRoundStateSubmission> submissions = new LinkedHashMap<>();

	/** 移出成员的最后资源仍须写回；不能因从可攻击列表移除而遗失第二轮承伤。 */
	private Map<String, List<BattleStateUnit>> withdrawn = new LinkedHashMap<>();

}

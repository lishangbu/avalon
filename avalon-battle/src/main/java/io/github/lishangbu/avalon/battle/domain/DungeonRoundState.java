package io.github.lishangbu.avalon.battle.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 可持久化的多人回合存档；读取、提交、到期处理必须由上层在同一副本锁下执行。 */
@Getter
@Setter
public class DungeonRoundState {

	private BattleState battle;

	private Instant deadline;

	private Map<String, Member> members = new LinkedHashMap<>();

	private Map<String, Submission> submissions = new LinkedHashMap<>();

	/** 移出成员的最后资源仍须写回；不能因从可攻击列表移除而遗失第二轮承伤。 */
	private Map<String, List<BattleState.Unit>> withdrawn = new LinkedHashMap<>();

	@Getter
	@Setter
	public static class Member {

		private int missedRounds;

		private boolean removed;

		private boolean leaving;

		/** 至少一次合法手动提交才具备本场奖励资格；离场仍取消资格。 */
		private boolean participated;

	}

	public record Submission(List<BattleState.Order> orders, Map<String, Integer> replacements) {
		public Submission {
			orders = List.copyOf(orders);
			replacements = Map.copyOf(replacements);
		}
	}

}

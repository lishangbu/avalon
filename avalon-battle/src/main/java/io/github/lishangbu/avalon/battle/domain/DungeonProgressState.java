package io.github.lishangbu.avalon.battle.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.time.Instant;

/**
 * 同一次副本的战间进度，不与单场战局混用。保存退出历史，防止把主动退出伪装成补员。 由上层持有副本锁后读写；生命、物资、奖励与本状态必须在同一事务提交。
 */
@Getter
@Setter
public class DungeonProgressState {

	private String dungeon;

	private String leader;

	/** 原始请求名单不随补员变化，仅用于开本请求防重，不参与当前队伍人数计算。 */
	private List<Long> initialOwners = List.of();

	private int cleared;

	private Long activeBattle;

	/** 现有成员明确发布后才允许外部玩家申请补入；补满后自动关闭。 */
	private boolean recruiting;

	private Map<String, Member> members = new LinkedHashMap<>();

	/** 保留已结算战局结果，重试不再次推进关卡；奖励另以同一战局编号防重。 */
	private Map<Long, String> resolvedBattles = new LinkedHashMap<>();

	@Getter
	@Setter
	public static class Member {

		/** 账号仅用于内部领域命令归属，不向其他玩家展示。 */
		private long owner;

		private long preparedRevision;

		/** 入本的四个战斗位及全部随身个体；战间重建按原位，不能使用临时换宠后的位次。 */
		private Map<String, Integer> roster = new LinkedHashMap<>();

		private List<String> carried = List.of();

		private boolean ready;

		/** 战间在线租约持久化；旧存档没有租约时必须重新准备，不能默认为在线。 */
		private Instant presenceUntil;

		/** 严格截止：到期即失效；查询仅投影，开战持锁后再次检查。 */
		public boolean presentAt(Instant now) {
			return presenceUntil != null && now.isBefore(presenceUntil);
		}

		private boolean exited;

		/** 所在路线节点：0 为入口，n 为第 n 场遭遇清理后的区域。 */
		private int location;

	}

}

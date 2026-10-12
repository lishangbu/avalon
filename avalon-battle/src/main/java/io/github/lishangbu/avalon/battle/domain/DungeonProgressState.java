package io.github.lishangbu.avalon.battle.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

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

	private Map<String, DungeonProgressStateMember> members = new LinkedHashMap<>();

	/** 保留已结算战局结果，重试不再次推进关卡；奖励另以同一战局编号防重。 */
	private Map<Long, String> resolvedBattles = new LinkedHashMap<>();

}

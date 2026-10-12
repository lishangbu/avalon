package io.github.lishangbu.avalon.battle.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** DungeonProgressState 的独立公开领域数据类型；保留原字段及约束，调用方直接引用此类型。 */
@Getter
@Setter
public class DungeonProgressStateMember {

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

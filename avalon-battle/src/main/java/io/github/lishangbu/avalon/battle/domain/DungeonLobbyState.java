package io.github.lishangbu.avalon.battle.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** 组队确认存档，不复用副本内准备状态；账号映射仅供服务端使用。 */
@Getter
@Setter
public class DungeonLobbyState {

	private long leader;

	private Long run;

	/** 非空表示一个候补者对现有副本的确认，run 仍只在实际入本后赋值。 */
	private Long targetRun;

	private boolean closed;

	/** 自动匹配创建的等待房间允许后续补员；手动邀请房间初始化为关闭自动补员。 */
	private boolean automatic;

	private Map<Long, DungeonLobbyStateMember> members = new LinkedHashMap<>();

}

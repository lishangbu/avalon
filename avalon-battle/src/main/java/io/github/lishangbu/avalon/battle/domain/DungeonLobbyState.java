package io.github.lishangbu.avalon.battle.domain;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** 组队确认存档，不复用副本内准备状态；账号映射仅供服务端使用。 */
@Getter
@Setter
public class DungeonLobbyState {

	private long leader;

	private Long run;

	/** 非空表示一个候补者对现有副本的确认，run 仍只在实际入本后赋值。 */
	private Long targetRun;

	private boolean closed;

	/** 自动匹配创建的等待房间；缺员时允许后续匹配者继续加入，旧档默认关闭。 */
	private boolean automatic;

	private Map<Long, Member> members = new LinkedHashMap<>();

	@Getter
	@Setter
	public static class Member {

		private Instant deadline;

		private boolean confirmed;

		private boolean ready;

		private long preparedRevision;

	}

}

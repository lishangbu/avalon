package io.github.lishangbu.avalon.battle.domain;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** DungeonLobbyState 的独立公开领域数据类型；保留原字段及约束，调用方直接引用此类型。 */
@Getter
@Setter
public class DungeonLobbyStateMember {

	private Instant deadline;

	private boolean confirmed;

	private boolean ready;

	private long preparedRevision;

}

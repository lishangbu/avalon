package io.github.lishangbu.avalon.battle.domain;

import lombok.Getter;
import lombok.Setter;

/** DungeonRoundState 的独立公开领域数据类型；保留原字段及约束，调用方直接引用此类型。 */
@Getter
@Setter
public class DungeonRoundStateMember {

	private int missedRounds;

	private boolean removed;

	private boolean leaving;

	/** 至少一次合法手动提交才具备本场奖励资格；离场仍取消资格。 */
	private boolean participated;

}

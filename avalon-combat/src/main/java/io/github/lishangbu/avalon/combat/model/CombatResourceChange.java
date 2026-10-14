package io.github.lishangbu.avalon.combat.model;

/** 待宿主原子应用的资源变更；owner来自服务端归属快照，不能直接相信客户端传入的资产归属。 */
public record CombatResourceChange(String kind, String owner, String reference, long amount, String target) {
	/** 类型、归属和数量必须完整；具体物品或捕获引用由宿主内容目录进一步校验。 */
	public CombatResourceChange {
		if (kind == null || kind.isBlank() || owner == null || owner.isBlank() || reference == null
				|| reference.isBlank() || amount <= 0)
			throw new IllegalArgumentException("战斗资源变更参数不完整");
	}
}

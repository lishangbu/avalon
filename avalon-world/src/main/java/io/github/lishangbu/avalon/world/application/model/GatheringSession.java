package io.github.lishangbu.avalon.world.application.model;

import java.util.Map;

/** 单角色仅一个当前采集会话；编号以字符串对外传输，revision用于心跳防重。 */
public record GatheringSession(String id, String node, String creature, String tool, String toolCode,
		int toolsRemaining, String location, long roundMillis, long progressMillis, Long lastSeenMillis, long revision,
		long completed, String status, boolean lastForeground, Map<String, Integer> supplies) {
	public GatheringSession {
		supplies = supplies == null ? Map.of() : Map.copyOf(supplies);
	}
}

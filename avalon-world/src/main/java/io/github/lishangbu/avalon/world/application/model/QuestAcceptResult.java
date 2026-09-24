package io.github.lishangbu.avalon.world.application.model;

import java.time.Instant;
import java.util.List;

/** acceptedAt表示第一次接取；不同请求再次接取的rewards为空，不能误导为重新领取。 */
public record QuestAcceptResult(String step, boolean accepted, String dialogue, List<String> rewards,
		Instant acceptedAt) {
	public QuestAcceptResult {
		rewards = List.copyOf(rewards);
	}
}

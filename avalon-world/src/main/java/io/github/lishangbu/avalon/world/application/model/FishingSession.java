package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.world.domain.FishingRulesState;

public record FishingSession(String id, String fish, String tool, String toolCode, boolean bound, String location,
		long seed, int skillLevel, int rodTier, long revision, boolean lastHeld, boolean lastForeground,
		FishingRulesState state, boolean broken) {
}

package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.world.domain.FishingRules;

public record FishingSession(String id, String fish, String tool, String toolCode, boolean bound, String location,
		long seed, int skillLevel, int rodTier, long revision, boolean lastHeld, boolean lastForeground,
		FishingRules.State state, boolean broken) {
}

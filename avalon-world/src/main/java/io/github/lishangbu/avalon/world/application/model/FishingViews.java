package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.content.life.FishingCatalog;
import io.github.lishangbu.avalon.world.domain.FishingRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FishingViews {

	private final FishingCatalog catalog;

	public Snapshot view(FishingSession session) {
		if (session == null)
			return null;
		int difficulty = catalog.fish(session.fish()).difficulty();
		return new Snapshot(session.id(), session.fish(), session.revision(), session.state(), session.broken(),
				FishingRules.fishPosition(session.seed(), difficulty, session.state().elapsedMillis()),
				FishingRules.tolerance(difficulty, session.skillLevel(), session.rodTier()));
	}

	public record Snapshot(String id, String fish, long revision, FishingRules.State state, boolean broken,
			double fishPosition, double tolerance) {
	}

}

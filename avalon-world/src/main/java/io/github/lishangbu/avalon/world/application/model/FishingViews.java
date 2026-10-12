package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.content.life.FishingCatalog;
import io.github.lishangbu.avalon.world.domain.FishingRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FishingViews {

	private final FishingCatalog catalog;

	public FishingSnapshot view(FishingSession session) {
		if (session == null)
			return null;
		int difficulty = catalog.fish(session.fish()).difficulty();
		return new FishingSnapshot(session.id(), session.fish(), session.revision(), session.state(), session.broken(),
				FishingRules.fishPosition(session.seed(), difficulty, session.state().elapsedMillis()),
				FishingRules.tolerance(difficulty, session.skillLevel(), session.rodTier()));
	}

}

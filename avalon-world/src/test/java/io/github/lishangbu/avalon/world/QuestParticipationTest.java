package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.persistence.entity.QuestParticipation;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

/** 接取与领奖分离，验证暂停期间不能交付及重接不会丢失首次接取信息。 */
class QuestParticipationTest {

	@Test
	void resumePreservesFirstAcceptanceAndDoesNotResetActiveWindowOnRetry() {
		var start = Instant.parse("2026-09-21T00:00:00Z");
		var state = QuestParticipation.begin(1, "SIDE_TEST", start);
		state.resume(start.plusSeconds(1));
		assertEquals(start, state.getActiveSince());
		assertEquals(0, state.getRevision());
		state.abandon(true);
		state.abandon(true);
		assertEquals(1, state.getRevision());
		assertThrows(BusinessException.class, state::complete);
		state.resume(start.plusSeconds(10));
		assertEquals(start, state.getFirstAcceptedAt());
		assertEquals(start.plusSeconds(10), state.getActiveSince());
		assertEquals(2, state.getRevision());
	}

	@Test
	void mainCannotBeAbandonedAndCompletionCannotBeReopened() {
		var state = QuestParticipation.begin(1, "MAIN_TEST", Instant.now());
		assertThrows(BusinessException.class, () -> state.abandon(false));
		assertEquals(QuestParticipation.Status.ACTIVE, state.getStatus());
		state.complete();
		state.complete();
		assertEquals(1, state.getRevision());
		assertThrows(BusinessException.class, () -> state.resume(Instant.now()));
		assertThrows(BusinessException.class, () -> state.abandon(true));
	}

}

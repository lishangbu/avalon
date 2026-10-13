package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.world.persistence.entity.QuestJournalState;
import io.github.lishangbu.avalon.world.persistence.entity.QuestJournalStateStatus;
import io.github.lishangbu.avalon.quest.QuestInstance;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 日志修订仅跟随实例状态转换，目标数量变化不会误伤放弃请求。 */
class QuestJournalStateTest {

	@Test
	void projectionOnlyChangesRevisionForLifecycleTransitions() {
		var at = Instant.now();
		var journal = QuestJournalState.begin(1, "TEST", at);
		var active = new QuestInstance("instance", "TEST", "ONCE", at, at, QuestStatus.ACTIVE, 1, Map.of(), Set.of(),
				Map.of(), Map.of(), Set.of(), Map.of());
		journal.synchronize(active);
		journal.synchronize(active);
		assertEquals(0, journal.getRevision());
		var paused = new QuestInstance("instance", "TEST", "ONCE", at, at, QuestStatus.PAUSED, 1, Map.of(), Set.of(),
				Map.of(), Map.of(), Set.of(), Map.of());
		journal.synchronize(paused);
		assertEquals(QuestJournalStateStatus.ABANDONED, journal.getStatus());
		assertEquals(1, journal.getRevision());
		journal.synchronize(paused);
		assertEquals(1, journal.getRevision());
	}

}

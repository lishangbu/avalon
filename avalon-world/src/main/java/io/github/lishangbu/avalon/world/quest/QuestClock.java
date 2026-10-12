package io.github.lishangbu.avalon.world.quest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** 宿主唯一任务时钟，测试可替换；微秒精度与PostgreSQL一致，避免跨事务回执时间比较漂移。 */
@Component
public class QuestClock {

	public Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

}

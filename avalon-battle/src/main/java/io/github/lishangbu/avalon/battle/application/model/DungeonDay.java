package io.github.lishangbu.avalon.battle.application.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** 以配置时区和重置小时划分业务日；跨日不改变已存在进度、物资或战局。 */
public final class DungeonDay {

	private DungeonDay() {
	}

	public static LocalDate at(Instant instant, String zone, int resetHour) {
		return instant.atZone(ZoneId.of(zone)).minusHours(resetHour).toLocalDate();
	}

}

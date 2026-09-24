package io.github.lishangbu.avalon.battle.persistence.repository;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.simple.JdbcClient;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

/** 副本每日发起计数，调用者持角色锁；PostgreSQL 原子 upsert 加入原事务，不独立提交。 */
@Repository
@RequiredArgsConstructor
public class DungeonDailyStartStore {

	private final JdbcClient jdbc;

	public int starts(long character, LocalDate day) {
		return jdbc.sql("SELECT starts FROM dungeon_daily_start WHERE character_id = :character AND day = :day")
			.param("character", character)
			.param("day", day)
			.query(Integer.class)
			.optional()
			.orElse(0);
	}

	public void useStart(long character, LocalDate day) {
		jdbc.sql(
				"INSERT INTO dungeon_daily_start(character_id, day, starts) VALUES (:character, :day, 1) ON CONFLICT (character_id, day) DO UPDATE SET starts = dungeon_daily_start.starts + 1")
			.param("character", character)
			.param("day", day)
			.update();
	}

}

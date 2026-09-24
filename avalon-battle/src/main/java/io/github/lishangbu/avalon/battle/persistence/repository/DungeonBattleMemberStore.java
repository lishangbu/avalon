package io.github.lishangbu.avalon.battle.persistence.repository;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.simple.JdbcClient;
import lombok.RequiredArgsConstructor;
import java.util.Collection;

/** 无实体参战关联使用同数据源 JDBC；插入前 flush 战局，保证外键可见并加入调用者事务。 */
@Repository
@RequiredArgsConstructor
public class DungeonBattleMemberStore {

	private final DungeonBattleRecordRepository battles;

	private final JdbcClient jdbc;

	public void addMember(long battle, long character) {
		battles.flush();
		jdbc.sql("INSERT INTO dungeon_battle_member(battle_id, character_id) VALUES (:battle, :character)")
			.param("battle", battle)
			.param("character", character)
			.update();
	}

	public boolean member(long battle, long character) {
		return jdbc
			.sql("SELECT count(*) FROM dungeon_battle_member WHERE battle_id=:battle AND character_id=:character")
			.param("battle", battle)
			.param("character", character)
			.query(Long.class)
			.single() == 1;
	}

	public Long latestParticipated(Collection<Long> resolved, long character) {
		if (resolved.isEmpty())
			return null;
		return jdbc.sql(
				"SELECT max(battle_id) FROM dungeon_battle_member WHERE character_id=:character AND battle_id IN (:resolved)")
			.param("character", character)
			.param("resolved", resolved)
			.query(Long.class)
			.optional()
			.orElse(null);
	}

}

package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.world.application.model.GatheringSession;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

/** 当前会话JSON持久化；写入与角色门禁、奖励、资源和工具消耗共用事务。 */
@Repository
@RequiredArgsConstructor
public class GatheringSessionRepository {

	private final JdbcClient jdbc;

	private final ObjectMapper json;

	public GatheringSession find(long character) {
		return jdbc.sql("SELECT state FROM gathering_session WHERE character_id=:character")
			.param("character", character)
			.query(String.class)
			.optional()
			.map(value -> json.readValue(value, GatheringSession.class))
			.orElse(null);
	}

	public void save(long character, GatheringSession state) {
		jdbc.sql(
				"INSERT INTO gathering_session(character_id,state) VALUES(:character,:state) ON CONFLICT (character_id) DO UPDATE SET state=EXCLUDED.state")
			.param("character", character)
			.param("state", json.writeValueAsString(state))
			.update();
	}

	public void delete(long character) {
		jdbc.sql("DELETE FROM gathering_session WHERE character_id=:character").param("character", character).update();
	}

}

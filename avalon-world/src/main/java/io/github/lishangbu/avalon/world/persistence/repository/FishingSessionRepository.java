package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.world.application.model.FishingSession;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class FishingSessionRepository {

	private final JdbcClient jdbc;

	private final ObjectMapper json;

	public FishingSession find(long character) {
		return jdbc.sql("SELECT state FROM fishing_session WHERE character_id=:character")
			.param("character", character)
			.query(String.class)
			.optional()
			.map(value -> json.readValue(value, FishingSession.class))
			.orElse(null);
	}

	public void save(long character, FishingSession session) {
		jdbc.sql(
				"INSERT INTO fishing_session(character_id,state) VALUES(:character,:state) ON CONFLICT(character_id) DO UPDATE SET state=EXCLUDED.state")
			.param("character", character)
			.param("state", json.writeValueAsString(session))
			.update();
	}

}

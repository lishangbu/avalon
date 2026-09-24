package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.world.application.model.FarmState;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

/** 农田状态随播种、收获与扩建事务保存；查询空农田不产生写操作。 */
@Repository
@RequiredArgsConstructor
public class FarmRepository {

	private final JdbcClient jdbc;

	private final ObjectMapper json;

	public FarmState find(long character) {
		return jdbc.sql("SELECT state FROM personal_farm WHERE character_id=:character")
			.param("character", character)
			.query(String.class)
			.optional()
			.map(state -> json.readValue(state, FarmState.class))
			.orElse(new FarmState(6, Map.of()));
	}

	public void save(long character, FarmState state) {
		jdbc.sql(
				"INSERT INTO personal_farm(character_id,state) VALUES(:character,:state) ON CONFLICT(character_id) DO UPDATE SET state=EXCLUDED.state")
			.param("character", character)
			.param("state", json.writeValueAsString(state))
			.update();
	}

}

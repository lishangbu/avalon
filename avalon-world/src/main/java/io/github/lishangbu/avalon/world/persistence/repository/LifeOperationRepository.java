package io.github.lishangbu.avalon.world.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** 所有写调用须持角色锁；回执与生产副作用加入同一事务。 */
@Repository
@RequiredArgsConstructor
public class LifeOperationRepository {

	private final JdbcClient jdbc;

	public Receipt prior(long character, String request) {
		return jdbc.sql(
				"SELECT payload,result FROM life_operation_receipt WHERE character_id=:character AND request_id=:request")
			.param("character", character)
			.param("request", request)
			.query((row, index) -> new Receipt(row.getString(1), row.getString(2)))
			.optional()
			.orElse(null);
	}

	public void record(long character, String request, String payload, String result) {
		jdbc.sql(
				"INSERT INTO life_operation_receipt(character_id,request_id,payload,result) VALUES (:character,:request,:payload,:result)")
			.param("character", character)
			.param("request", request)
			.param("payload", payload)
			.param("result", result)
			.update();
	}

	public record Receipt(String payload, String result) {
	}

}

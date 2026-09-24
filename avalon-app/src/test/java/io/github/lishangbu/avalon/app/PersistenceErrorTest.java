package io.github.lishangbu.avalon.app;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 名称冲突可由玩家纠正；内部主键或奖励来源冲突应作为服务故障保留日志。
 */
class PersistenceErrorTest {

	/**
	 * 已知账号/角色唯一约束映射409，主键和未知内部约束映射500。构造SQLSTATE与约束名称，确认分类不是所有23505一律作为玩家重名。
	 */
	@Test
	void classifiesKnownConstraintsOnly() {
		var mapper = new PersistenceExceptionMapper();
		for (String constraint : new String[] { "account_username_key", "player_character_name_key",
				"player_character_account_id_key" }) {
			{
				var response = mapper.toResponse(conflict(constraint));
				assertEquals(409, response.getStatusCode().value());
			}
		}
		for (String constraint : new String[] { "account_pkey", "owned_item_character_id_source_key",
				"unknown_constraint" }) {
			{
				var response = mapper.toResponse(conflict(constraint));
				assertEquals(500, response.getStatusCode().value());
			}
		}
	}

	ConstraintViolationException conflict(String name) {
		return new ConstraintViolationException("test", new SQLException("duplicate", "23505"), name);
	}

}

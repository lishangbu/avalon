package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** 生活技能存储边界；所有写入由应用服务先取得角色锁。 */
@Repository
@RequiredArgsConstructor
public class LifeSkillRepository {

	private final JdbcClient jdbc;

	public Map<LifeSkillRulesSkill, Long> owned(long character) {
		return jdbc.sql("SELECT skill, half_points FROM life_skill WHERE character_id=:character")
			.param("character", character)
			.query((row, index) -> Map.entry(LifeSkillRulesSkill.valueOf(row.getString(1)), row.getLong(2)))
			.list()
			.stream()
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public String prior(long character, String request) {
		return jdbc.sql(
				"SELECT action || ':' || skill FROM life_skill_receipt WHERE character_id=:character AND request_id=:request")
			.param("character", character)
			.param("request", request)
			.query(String.class)
			.optional()
			.orElse(null);
	}

	public void learn(long character, LifeSkillRulesSkill skill) {
		jdbc.sql("INSERT INTO life_skill(character_id,skill,half_points) VALUES (:character,:skill,0)")
			.param("character", character)
			.param("skill", skill.name())
			.update();
	}

	public void forget(long character, LifeSkillRulesSkill skill) {
		jdbc.sql("DELETE FROM life_skill WHERE character_id=:character AND skill=:skill")
			.param("character", character)
			.param("skill", skill.name())
			.update();
	}

	public void record(long character, String request, String action, LifeSkillRulesSkill skill) {
		jdbc.sql(
				"INSERT INTO life_skill_receipt(character_id,request_id,action,skill) VALUES (:character,:request,:action,:skill)")
			.param("character", character)
			.param("request", request)
			.param("action", action)
			.param("skill", skill.name())
			.update();
	}

	public void progress(long character, LifeSkillRulesSkill skill, long halfPoints) {
		if (jdbc.sql("UPDATE life_skill SET half_points=:points WHERE character_id=:character AND skill=:skill")
			.param("points", halfPoints)
			.param("character", character)
			.param("skill", skill.name())
			.update() != 1)
			throw new IllegalStateException("尚未学习该生活技能");
	}

}

package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.world.application.model.LifeSkillEntry;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 普通读取不加写锁；统一投影半点熟练度及下一等级累计门槛。 */
@Component
@RequiredArgsConstructor
public class LifeSkillQueryService {

	private final CharacterQueryService characters;

	private final LifeSkillRepository skills;

	public List<LifeSkillEntry> list(long owner) {
		var owned = skills.owned(characters.requireId(owner));
		return LifeSkillRules.skills().stream().map(skill -> {
			long points = owned.getOrDefault(skill, 0L);
			int level = LifeSkillRules.level(points);
			return new LifeSkillEntry(skill.name(), skill.title, skill.profession, owned.containsKey(skill), level,
					points / 2.0, level == 10 ? null : LifeSkillRules.threshold(level + 1));
		}).toList();
	}

}

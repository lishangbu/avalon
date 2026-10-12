package io.github.lishangbu.avalon.battle.application.query;

import io.github.lishangbu.avalon.battle.application.model.AcademyTrainingCourse;
import io.github.lishangbu.avalon.battle.application.model.AcademyTrainingCourses;
import io.github.lishangbu.avalon.battle.application.model.AcademyTrainingPartner;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.AcademyTrainingCatalog;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 根据服务器事实投影可参加课程；前端不接触解锁事实表达式或敌方数值。 */
@Component
@RequiredArgsConstructor
public class AcademyTrainingQueryService {

	private final CharacterQueryService characters;

	private final CharacterFactQueryService facts;

	private final AcademyTrainingCatalog catalog;

	@Transactional
	public AcademyTrainingCourses available(long owner) {
		var character = characters.find(owner);
		if (character == null || !character.location().equals(catalog.definition().location()))
			return new AcademyTrainingCourses(false, List.of(), List.of());
		var adventure = characters.adventure(owner);
		if (adventure.battle() != null || adventure.dungeon() != null || adventure.course() != null)
			return new AcademyTrainingCourses(false, List.of(), List.of());
		var completed = facts.codes(owner);
		var available = catalog.definition()
			.courses()
			.entrySet()
			.stream()
			.filter(entry -> completed.contains(entry.getValue().requires()))
			.sorted(Map.Entry.comparingByKey())
			.map(entry -> new AcademyTrainingCourse(entry.getKey(), entry.getValue().name(),
					entry.getValue().objective(), entry.getValue().goal().equals("SWAP"),
					completed.contains("TRAINING:" + entry.getKey())))
			.toList();
		if (available.isEmpty())
			return new AcademyTrainingCourses(false, List.of(), List.of());
		var partners = catalog.definition()
			.partners()
			.stream()
			.map(pet -> new AcademyTrainingPartner(pet.id(), pet.name(), pet.element(), pet.stats(), pet.skills()))
			.toList();
		return new AcademyTrainingCourses(true, partners, available);
	}

}

package io.github.lishangbu.avalon.battle.application.query;

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
	public Courses available(long owner) {
		var character = characters.find(owner);
		if (character == null || !character.location().equals(catalog.definition().location()))
			return new Courses(false, List.of(), List.of());
		var adventure = characters.adventure(owner);
		if (adventure.battle() != null || adventure.dungeon() != null || adventure.course() != null)
			return new Courses(false, List.of(), List.of());
		var completed = facts.codes(owner);
		var available = catalog.definition()
			.courses()
			.entrySet()
			.stream()
			.filter(entry -> completed.contains(entry.getValue().requires()))
			.sorted(Map.Entry.comparingByKey())
			.map(entry -> new Course(entry.getKey(), entry.getValue().name(), entry.getValue().objective(),
					entry.getValue().goal().equals("SWAP"), completed.contains("TRAINING:" + entry.getKey())))
			.toList();
		if (available.isEmpty())
			return new Courses(false, List.of(), List.of());
		var partners = catalog.definition()
			.partners()
			.stream()
			.map(pet -> new Partner(pet.id(), pet.name(), pet.element(), pet.stats(), pet.skills()))
			.toList();
		return new Courses(true, partners, available);
	}

	public record Courses(boolean available, List<Partner> partners, List<Course> courses) {
	}

	public record Course(String id, String name, String goal, boolean selectPartners, boolean completed) {
	}

	public record Partner(String id, String name, String element, List<Integer> stats, List<String> skills) {
	}

}

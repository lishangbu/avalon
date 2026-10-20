package io.github.lishangbu.avalon.battle.application.query;

import io.github.lishangbu.avalon.battle.application.WorkshopCourseProjector;
import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseCourse;
import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseCourses;
import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseView;
import io.github.lishangbu.avalon.battle.domain.WorkshopCourseState;
import io.github.lishangbu.avalon.battle.persistence.entity.WorkshopCourseSession;
import io.github.lishangbu.avalon.battle.persistence.repository.WorkshopCourseSessionRepository;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 只读恢复完整临时进度；查询不创建会话、不抽样、不改变正式资产。 */
@Component
@RequiredArgsConstructor
public class WorkshopCourseQueryService {

	private final CharacterQueryService characters;

	private final CharacterFactQueryService facts;

	private final WorkshopCourseSessionRepository sessions;

	private final WorkshopCourseProjector projector;

	private final ObjectMapper json;

	@Transactional
	public WorkshopCourseCourses courses(long owner) {
		var character = characters.find(owner);
		if (character == null)
			return new WorkshopCourseCourses(null, List.of());
		var active = sessions.active(Long.parseLong(character.id()));
		var adventure = characters.adventure(owner);
		var completed = facts.codes(owner);
		boolean free = active == null && adventure.battle() == null && adventure.dungeon() == null
				&& characters.lobby(owner) == null && completed.contains("QUEST:O012");
		boolean academy = free && character.location().equals("ACADEMY");
		boolean academyPets = free
				&& (character.location().equals("ACADEMY") || character.location().equals("ACADEMY_PETS"));
		return new WorkshopCourseCourses(active == null ? null : view(active),
				List.of(new WorkshopCourseCourse("WORKSHOP_SKILLS", "技能书里的选择", academy,
						completed.contains("TRAINING:WORKSHOP_SKILLS"),
						academy ? null : "需完成前往诺姆的旅程并到学院主楼，且不处于其他战斗、组队或课程"),
						new WorkshopCourseCourse("WORKSHOP_GEAR", "行囊里的分寸", academyPets,
								completed.contains("TRAINING:WORKSHOP_GEAR"),
								academyPets ? null : "需完成前往诺姆的旅程并到学院宠物区或主楼，且不处于其他战斗、组队或课程")));
	}

	@Transactional
	public WorkshopCourseView get(long owner, long id) {
		long character = characters.requireId(owner);
		var session = sessions.get(id);
		if (session == null || session.getCharacterId() != character)
			throw new BusinessException(404, "课程会话不存在");
		return view(session);
	}

	private WorkshopCourseView view(WorkshopCourseSession session) {
		return projector.project(session.getId(), session.getCourse(), session.getStatus(),
				json.readValue(session.getState(), WorkshopCourseState.class));
	}

}

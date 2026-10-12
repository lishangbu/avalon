package io.github.lishangbu.avalon.battle.api;

import io.github.lishangbu.avalon.battle.api.model.WorkshopCourseStart;
import io.github.lishangbu.avalon.battle.application.command.WorkshopCourseCommandService;
import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseAction;
import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseCourses;
import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseView;
import io.github.lishangbu.avalon.battle.application.query.WorkshopCourseQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 学院战外练习API；没有直接传入技能、资源、成功事实或正式资产编号的入口。 */
@RestController
@RequestMapping("/api/academy/workshop")
@RequiredArgsConstructor
public class WorkshopCourseResource {

	private final CurrentAccount identity;

	private final WorkshopCourseCommandService commands;

	private final WorkshopCourseQueryService queries;

	@GetMapping
	public WorkshopCourseCourses courses() {
		return queries.courses(identity.id());
	}

	@GetMapping("/{id}")
	public WorkshopCourseView get(@PathVariable long id) {
		return queries.get(identity.id(), id);
	}

	@PostMapping
	public WorkshopCourseView start(@RequestBody(required = false) WorkshopCourseStart request) {
		if (request == null)
			throw new BusinessException(400, "缺少课程参数");
		return commands.start(identity.id(), request.course(), request.requestId());
	}

	@PostMapping("/{id}/actions")
	public WorkshopCourseView act(@PathVariable long id, @RequestBody(required = false) WorkshopCourseAction request) {
		return commands.act(identity.id(), id, request);
	}

}

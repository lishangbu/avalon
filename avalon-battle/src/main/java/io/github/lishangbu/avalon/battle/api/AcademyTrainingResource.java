package io.github.lishangbu.avalon.battle.api;

import io.github.lishangbu.avalon.battle.application.command.TrainingCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.application.query.AcademyTrainingQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 学院借用课程入口；真实回合仍使用普通战斗接口及相同服务器校验。 */
@RestController
@RequestMapping("/api/battle/academy")
@RequiredArgsConstructor
public class AcademyTrainingResource {

	private final CurrentAccount identity;

	private final TrainingCommandService commands;

	private final AcademyTrainingQueryService queries;

	@GetMapping
	public AcademyTrainingQueryService.Courses courses() {
		return queries.available(identity.id());
	}

	@PostMapping
	public BattleView start(@RequestBody(required = false) Start request) {
		if (request == null || request.partners() != null && request.partners().stream().anyMatch(Objects::isNull))
			throw new BusinessException(400, "缺少课程参数或伙伴编号");
		return commands.startAcademy(identity.id(), request.course(), request.partners(), request.requestId());
	}

	public record Start(String course, List<String> partners, String requestId) {
	}

	@PostMapping("/{id}/capture-info")
	public BattleView captureInfo(@PathVariable long id, @RequestBody(required = false) CaptureInfo request) {
		if (request == null || request.ball() == null)
			throw new BusinessException(400, "请选择训练封印球");
		return commands.captureInfo(identity.id(), id, request.ball());
	}

	public record CaptureInfo(String ball) {
	}

}

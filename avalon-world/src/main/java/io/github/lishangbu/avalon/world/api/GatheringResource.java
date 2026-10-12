package io.github.lishangbu.avalon.world.api;

import io.github.lishangbu.avalon.platform.CurrentAccount;
import io.github.lishangbu.avalon.world.application.command.GatheringCommandService;
import io.github.lishangbu.avalon.world.application.model.GatheringBeat;
import io.github.lishangbu.avalon.world.application.model.GatheringSession;
import io.github.lishangbu.avalon.world.application.model.GatheringStart;
import io.github.lishangbu.avalon.world.application.model.GatheringStop;
import io.github.lishangbu.avalon.world.application.model.GatheringView;
import io.github.lishangbu.avalon.world.application.query.GatheringQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 身份由认证会话解析；请求不能指定角色、时间或产物。 */
@RestController
@RequestMapping("/api/life/gathering")
@RequiredArgsConstructor
public class GatheringResource {

	private final CurrentAccount identity;

	private final GatheringQueryService queries;

	private final GatheringCommandService commands;

	@GetMapping
	public GatheringView get() {
		return queries.view(identity.id());
	}

	@PostMapping("/start")
	public GatheringSession start(@RequestBody(required = false) GatheringStart request) {
		return commands.begin(identity.id(), request);
	}

	@PostMapping("/heartbeat")
	public GatheringSession heartbeat(@RequestBody(required = false) GatheringBeat request) {
		return commands.heartbeat(identity.id(), request);
	}

	@PostMapping("/stop")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void stop(@RequestBody(required = false) GatheringStop request) {
		commands.stop(identity.id(), request);
	}

}

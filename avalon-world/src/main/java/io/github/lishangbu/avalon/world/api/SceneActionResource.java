package io.github.lishangbu.avalon.world.api;

import io.github.lishangbu.avalon.platform.CurrentAccount;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import io.github.lishangbu.avalon.world.application.model.SceneActionResult;
import io.github.lishangbu.avalon.world.application.query.SceneActionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 场景动作身份来自登录账号；仅允许稳定动作编号、版本与受限选项进入命令。 */
@RestController
@RequestMapping("/api/scene-actions")
@RequiredArgsConstructor
public class SceneActionResource {

	private final CurrentAccount identity;

	private final SceneActionCommandService commands;

	private final SceneActionQueryService queries;

	@GetMapping
	public SceneActionQueryService.State state() {
		return queries.state(identity.id());
	}

	@PostMapping("/{actionId}")
	public SceneActionResult execute(@PathVariable String actionId,
			@RequestBody(required = false) SceneActionCommandService.Request request) {
		return commands.execute(identity.id(), actionId, request);
	}

}

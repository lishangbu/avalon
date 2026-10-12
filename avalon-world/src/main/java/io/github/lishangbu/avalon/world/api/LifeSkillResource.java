package io.github.lishangbu.avalon.world.api;

import io.github.lishangbu.avalon.platform.CurrentAccount;
import io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService;
import io.github.lishangbu.avalon.world.application.model.LifeSkillRequest;
import io.github.lishangbu.avalon.world.application.model.LifeSkillEntry;
import io.github.lishangbu.avalon.world.application.query.LifeSkillQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 身份来自会话，客户端不能指定角色或提交熟练度奖励。 */
@RestController
@RequestMapping("/api/life/skills")
@RequiredArgsConstructor
public class LifeSkillResource {

	private final CurrentAccount identity;

	private final LifeSkillQueryService queries;

	private final LifeSkillCommandService commands;

	@GetMapping
	public List<LifeSkillEntry> get() {
		return queries.list(identity.id());
	}

	@PostMapping("/learn")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void learn(@RequestBody(required = false) LifeSkillRequest request) {
		commands.change(identity.id(), request, false);
	}

	@PostMapping("/forget")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void forget(@RequestBody(required = false) LifeSkillRequest request) {
		commands.change(identity.id(), request, true);
	}

}

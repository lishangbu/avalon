package io.github.lishangbu.avalon.identity.api;

import io.github.lishangbu.avalon.identity.application.command.NavigationCommandService;
import io.github.lishangbu.avalon.identity.application.model.SessionViewer;
import io.github.lishangbu.avalon.identity.application.query.NavigationQueryService;
import io.github.lishangbu.avalon.identity.security.CurrentSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 导航偏好只影响页面入口，不提供地图移动或设施授权。 */
@RestController
@RequestMapping("/api/account/navigation")
@RequiredArgsConstructor
public class NavigationResource {

	private final NavigationQueryService queries;

	private final NavigationCommandService commands;

	private final CurrentSession identity;

	@GetMapping
	public List<String> read() {
		return queries.read(viewer().accountId());
	}

	@PutMapping
	public List<String> save(@RequestBody(required = false) List<String> slots) {
		return commands.save(viewer().accountId(), slots);
	}

	private SessionViewer viewer() {
		return identity.viewer();
	}

}

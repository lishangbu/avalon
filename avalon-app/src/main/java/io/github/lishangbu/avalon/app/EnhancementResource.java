package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.inventory.application.command.EnhancementCommandService;
import io.github.lishangbu.avalon.inventory.application.command.EnhancementPreviewService;
import io.github.lishangbu.avalon.inventory.application.model.CultivationMaterialMode;
import io.github.lishangbu.avalon.inventory.application.model.EnhancementAccess;
import io.github.lishangbu.avalon.inventory.application.model.EnhancementPreview;
import io.github.lishangbu.avalon.inventory.application.model.EnhancementRequest;
import io.github.lishangbu.avalon.inventory.application.model.EnhancementResult;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 强化仅接受目标编号、材料模式和服务端预览，不接受客户端自定概率或跳过费用。 */
@RestController
@RequestMapping("/api/enhancement")
@RequiredArgsConstructor
public class EnhancementResource {

	private final CurrentAccount identity;

	private final EnhancementPreviewService queries;

	private final EnhancementCommandService commands;

	private long owner() {
		return identity.id();
	}

	@GetMapping("/access")
	public EnhancementAccess access() {
		return queries.access(owner());
	}

	@GetMapping("/{id}")
	public EnhancementPreview preview(@PathVariable("id") long id,
			@RequestParam(value = "mode", required = false) CultivationMaterialMode mode) {
		return queries.preview(owner(), id, mode);
	}

	@PostMapping("/{id}")
	public EnhancementResult enhance(@PathVariable("id") long id,
			@RequestBody(required = false) EnhancementRequest request) {
		return commands.enhance(owner(), id, request);
	}

}

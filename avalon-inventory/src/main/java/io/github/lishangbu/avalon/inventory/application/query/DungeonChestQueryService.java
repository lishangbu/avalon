package io.github.lishangbu.avalon.inventory.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.inventory.application.model.DungeonChestView;
import io.github.lishangbu.avalon.inventory.persistence.repository.DungeonChestRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 只返回本人的未领取宝箱；箱内结果保存在服务端，不因查询或跨日再次抽奖。 */
@Component
@RequiredArgsConstructor
public class DungeonChestQueryService {

	private final CharacterQueryService characters;

	private final DungeonChestRepository chests;

	@Transactional
	public List<DungeonChestView> list(long owner) {
		long character = characters.requireId(owner);
		return chests.unclaimed(character)
			.stream()
			.map(chest -> new DungeonChestView(chest.getId().toString(), chest.getName(), chest.getCreatedAt()))
			.toList();
	}

}

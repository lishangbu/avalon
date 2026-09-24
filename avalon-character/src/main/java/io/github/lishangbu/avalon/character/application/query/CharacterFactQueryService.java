package io.github.lishangbu.avalon.character.application.query;

import io.github.lishangbu.avalon.character.persistence.repository.CharacterFactRepository;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * 只读事实集合供任务条件与地图门禁使用，不通过读取补写进度或获取角色写锁。
 */
@Component
@RequiredArgsConstructor
public class CharacterFactQueryService {

	private final CharacterQueryService characters;

	private final CharacterFactRepository facts;

	@Transactional
	public Set<String> codes(long owner) {
		return facts.codes(characters.requireId(owner));
	}

}

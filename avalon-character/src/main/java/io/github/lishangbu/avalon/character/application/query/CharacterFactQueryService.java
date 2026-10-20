package io.github.lishangbu.avalon.character.application.query;

import io.github.lishangbu.avalon.character.persistence.repository.CharacterFactRepository;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.Map;
import java.time.Instant;
import java.util.stream.Collectors;

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

	/**
	 * 返回当前账号角色的首次事实时间。所有者仍由角色服务解析，不接受客户端传入其他角色编号。
	 * 时间直接取持久化事实，不使用读取时间替代观察时间；场景列表可批量投影，避免逐行动查询数据库。
	 */
	@Transactional
	public Map<String, Instant> recordedTimes(long owner) {
		return facts.findAllByCharacterId(characters.requireId(owner))
			.stream()
			.collect(Collectors.toMap(fact -> fact.getCode(), fact -> fact.getCreatedAt()));
	}

}

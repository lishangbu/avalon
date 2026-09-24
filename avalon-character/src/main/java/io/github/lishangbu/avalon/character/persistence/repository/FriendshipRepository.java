package io.github.lishangbu.avalon.character.persistence.repository;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.PageRequest;

import io.github.lishangbu.avalon.character.persistence.entity.Friendship;
import org.springframework.data.jpa.repository.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import jakarta.persistence.LockModeType;
import java.util.List;

/** 关系锁只保护本关系；查询始终限定当前角色，不能根据雪花编号取得他人的好友列表。 */
public interface FriendshipRepository extends EntityRepository<Friendship, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Friendship e where firstCharacterId = ?1 and secondCharacterId = ?2")
	Friendship pair(long first, long second);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Friendship e where id = ?1 and (firstCharacterId = ?2 or secondCharacterId = ?2)")
	Friendship lockOwned(long id, long character);

	/** 多取一条判断是否还有下一页，避免不断增长的好友关系导致无界响应。 */
	default List<Friendship> page(long character, long after) {
		return pageRows(character, after, PageRequest.of(0, 51));
	}

	@Query("select e from Friendship e where (firstCharacterId = ?1 or secondCharacterId = ?1) and id > ?2 order by id")
	List<Friendship> pageRows(long character, long after, Pageable page);

	@Query("select (count(e) > 0) from Friendship e where firstCharacterId = ?1 and secondCharacterId = ?2 and accepted = true")
	boolean accepted(long first, long second);

}

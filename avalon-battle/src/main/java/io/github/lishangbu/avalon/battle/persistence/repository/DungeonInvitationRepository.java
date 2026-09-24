package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface DungeonInvitationRepository extends EntityRepository<DungeonInvitation, Long> {

	@Query(value = "from DungeonInvitation where senderId = ?1 and requestId = ?2")
	DungeonInvitation request(long sender, String request);

	@Query(value = "from DungeonInvitation where lobbyId = ?1 and recipientId = ?2 and status = 'PENDING'")
	DungeonInvitation pending(long lobby, long recipient);

	@Query(value = "select lobbyId from DungeonInvitation where id = ?1 and recipientId = ?2")
	Long lobby(long id, long recipient);

	@Query(value = "from DungeonInvitation where recipientId = ?1 and id > ?2 and status = 'PENDING' and expiresAt > ?3 order by id")
	List<DungeonInvitation> inboxRows(long recipient, long after, Instant now, Pageable page);

	default List<DungeonInvitation> inbox(long recipient, long after, Instant now) {
		return inboxRows(recipient, after, now, PageRequest.of(0, 50));
	}

}

package io.github.lishangbu.avalon.platform.persistence;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;
import jakarta.persistence.LockModeType;

/** 通用部分仅提供空值查询和写锁；业务筛选由具体仓库声明，不开放任意查询字符串。 */
@NoRepositoryBean
public interface EntityRepository<T, ID> extends JpaRepository<T, ID> {

	default T get(ID id) {
		return findById(id).orElse(null);
	}

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from #{#entityName} e where e.id = ?1")
	T lock(ID id);

}

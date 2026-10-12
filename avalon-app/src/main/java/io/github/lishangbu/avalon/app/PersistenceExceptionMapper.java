package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.platform.BusinessExceptionMapperErrorBody;
import jakarta.persistence.PersistenceException;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 数据库仍是并发唯一性的最终裁判；只把唯一冲突转为可公开提示，其他错误记录并隐藏细节。
 */
@RestControllerAdvice
public class PersistenceExceptionMapper {

	private static final Map<String, String> CONFLICTS = Map.of("account_username_key", "账号已存在",
			"player_character_name_key", "角色名已被使用", "player_character_account_id_key", "已有角色，请刷新后重试");

	private static final Logger LOG = LoggerFactory.getLogger(PersistenceExceptionMapper.class);

	@ExceptionHandler({ PersistenceException.class, DataAccessException.class })
	public ResponseEntity<BusinessExceptionMapperErrorBody> toResponse(RuntimeException failure) {
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException constraint && "23505".equals(constraint.getSQLState())) {
				String message = CONFLICTS
					.get(constraint.getConstraintName() == null ? "" : constraint.getConstraintName());
				if (message != null)
					return ResponseEntity.status(409).body(new BusinessExceptionMapperErrorBody(message));
				// 主键、内部奖励来源或未知约束不冒充玩家可修正的重名问题。
				break;
			}
		}
		LOG.error("数据库操作失败", failure);
		return ResponseEntity.internalServerError().body(new BusinessExceptionMapperErrorBody("操作暂时无法完成，请稍后重试"));
	}

}

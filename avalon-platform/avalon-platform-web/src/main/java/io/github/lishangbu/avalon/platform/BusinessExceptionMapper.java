package io.github.lishangbu.avalon.platform;

import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

/** 业务错误保持原有 HTTP 状态及 message 字段；请求格式错误不泄漏内部结构。 */
@RestControllerAdvice
public class BusinessExceptionMapper {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorBody> toResponse(BusinessException error) {
		return ResponseEntity.status(error.status()).body(new ErrorBody(error.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorBody> malformed(Exception error) {
		return ResponseEntity.badRequest().body(new ErrorBody("请求参数格式不正确"));
	}

	/** 路径和查询参数转换失败沿用原 API 的 404，JSON 请求体格式错误仍为 400。 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorBody> invalidParameter(Exception error) {
		return ResponseEntity.status(404).body(new ErrorBody("请求参数无效"));
	}

	public record ErrorBody(String message) {
	}

}

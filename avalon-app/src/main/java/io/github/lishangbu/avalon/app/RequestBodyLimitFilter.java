package io.github.lishangbu.avalon.app;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 保留原服务的 32 KiB 请求体上限。不能只相信 Content-Length， 分块传输也最多读取上限加一个字节；缓存后的流供 MVC 正常解析。
 */
@Component
public class RequestBodyLimitFilter extends OncePerRequestFilter {

	private static final int MAX_BYTES = 32 * 1024;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (request.getContentLengthLong() > MAX_BYTES) {
			reject(response);
			return;
		}
		byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
		if (body.length > MAX_BYTES) {
			reject(response);
			return;
		}
		chain.doFilter(new HttpServletRequestWrapper(request) {
			@Override
			public ServletInputStream getInputStream() {
				var input = new ByteArrayInputStream(body);
				return new ServletInputStream() {
					@Override
					public int read() {
						return input.read();
					}

					@Override
					public int read(byte[] bytes, int offset, int length) {
						return input.read(bytes, offset, length);
					}

					@Override
					public boolean isFinished() {
						return input.available() == 0;
					}

					@Override
					public boolean isReady() {
						return true;
					}

					@Override
					public void setReadListener(ReadListener listener) {
						throw new UnsupportedOperationException("API 请求使用同步虚拟线程读取");
					}
				};
			}

			@Override
			public BufferedReader getReader() {
				return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
			}
		}, response);
	}

	private void reject(HttpServletResponse response) throws IOException {
		response.setStatus(413);
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write("{\"message\":\"请求内容过大\"}");
	}

}

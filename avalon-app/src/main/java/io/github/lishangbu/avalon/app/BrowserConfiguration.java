package io.github.lishangbu.avalon.app;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;
import java.io.IOException;

/**
 * HTML5 路由回退由静态资源解析层处理：先查真实文件，再回退无扩展名的页面。 不重复维护前端路由表；未知 API、健康端点和缺失资源必须保持 404。
 */
@Configuration
public class BrowserConfiguration implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
			.addResourceLocations("classpath:/static/")
			.resourceChain(true)
			.addResolver(new PathResourceResolver() {
				@Override
				protected Resource getResource(String path, Resource location) throws IOException {
					Resource resource = super.getResource(path, location);
					if (resource != null)
						return resource;
					if (path.equals("api") || path.startsWith("api/") || path.equals("actuator")
							|| path.startsWith("actuator/") || path.contains("."))
						return null;
					return super.getResource("index.html", location);
				}
			});
	}

}

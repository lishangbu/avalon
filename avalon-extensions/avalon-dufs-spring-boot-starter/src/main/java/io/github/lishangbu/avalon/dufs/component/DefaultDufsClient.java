package io.github.lishangbu.avalon.dufs.component;

import io.github.lishangbu.avalon.dufs.autoconfiguration.DufsAutoConfiguration;
import io.github.lishangbu.avalon.dufs.exception.DirectoryAlreadyExistsException;
import io.github.lishangbu.avalon.dufs.exception.PathNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * 默认的DUFS客户端
 *
 * @author lishangbu
 * @since 2025/8/11
 */
@Component
public class DefaultDufsClient implements DufsClient {

  @Qualifier(DufsAutoConfiguration.DUFS_REST_CLIENT_BEAN_NAME)
  private final RestClient restClient;

  public DefaultDufsClient(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public void upload(MultipartFile file, String... destination) throws IOException {
    restClient.put()
      .uri(UriComponentsBuilder.newInstance()
        .pathSegment(destination)  // Add all destination parts as path segments
        .pathSegment(file.getName())  // Add file name as a path segment
        .build()
        .toUri())
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .body(file.getResource())
      .retrieve()
      .toBodilessEntity();
  }

  /**
   * 创建文件夹
   *
   * @param path 要创建的文件夹路径
   * @throws DirectoryAlreadyExistsException 如果文件夹已经存在,抛出异常
   */
  @Override
  public void mkdir(String path) {
    restClient.method(HttpMethod.valueOf("MKCOL")).uri(URI.create(path))
      .exchange((request, response) -> {
        if (response.getStatusCode().is4xxClientError()) {
          String result = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
          if ("Already exists".equals(result)) {
            throw new DirectoryAlreadyExistsException(String.format("路径为[%s]的文件夹已经存在", path));
          }
        }
        return response;
      });
  }

  @Override
  public void delete(String path) {
    restClient.delete().uri(URI.create(path)).exchange((request, response) -> {
      if (response.getStatusCode().is4xxClientError()) {
        String result = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        if ("Not Found".equals(result)) {
          throw new PathNotFoundException(String.format("删除路径为[%s]的文件/文件夹失败,找不到对应资源", path));
        }
      }
      return response;
    });

  }
}

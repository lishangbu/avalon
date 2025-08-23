package io.github.lishangbu.avalon.dufs.component;

import io.github.lishangbu.avalon.dufs.exception.DirectoryAlreadyExistsException;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

/**
 * dufs客户端
 *
 * @author lishangbu
 * @since 2025/8/11
 */
public interface DufsClient {

  /**
   * 上传文件到指定路径
   *
   * @param file 文件
   * @param destination 目标路径
   */
  void upload(MultipartFile file, String... destination) throws IOException;

  /**
   * 创建文件夹
   *
   * @param path 要创建的文件夹路径
   * @throws DirectoryAlreadyExistsException 如果文件夹已经存在,抛出异常
   */
  void mkdir(String path) throws DirectoryAlreadyExistsException;

  /**
   * 删除文件/文件夹
   *
   * @param path 要删除的文件/文件夹路径
   */
  void delete(String path);
}

package io.github.lishangbu.avalon.pokeapi.component;

import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.properties.PokeApiProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import tools.jackson.databind.json.JsonMapper;

/// 默认的 PokeApi 服务实现
///
/// 从本地克隆或读取 PokeAPI 的数据仓库文件并反序列化为目标实体
///
/// @author lishangbu
/// @since 2025/5/21
@Slf4j
public class DefaultPokeApiService implements PokeApiService {

    /// 本地仓库中存储数据的具体文件路径
    private static final String LOCAL_GIT_REPO_FIRST_FILE_DIR_NAME = "data";

    private static final String LOCAL_GIT_REPO_SECOND_FILE_DIR_NAME = "api";
    private static final String LOCAL_GIT_REPO_THIRD_FILE_DIR_NAME = "v2";

    /// 具体存储数据的文件名称
    private static final String FILE_NAME = "index.json";

    private final PokeApiProperties properties;
    private final JsonMapper jsonMapper;

    public DefaultPokeApiService(PokeApiProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    /// 通过指定的 URI 和参数获取指定类型的数据实体
    ///
    /// @param typeEnum 资源类型
    /// @param id       资源对应的唯一 ID
    /// @return 指定类型的数据实体
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getEntityFromUri(PokeDataTypeEnum typeEnum, Integer id) {
        log.debug("获取类型为[{}]的数据，参数: [{}]", typeEnum, id);
        checkoutGitRepoIfNotExists();
        try {
            Path path =
                    Paths.get(
                            this.properties.getLocalRepoDir(),
                            LOCAL_GIT_REPO_FIRST_FILE_DIR_NAME,
                            LOCAL_GIT_REPO_SECOND_FILE_DIR_NAME,
                            LOCAL_GIT_REPO_THIRD_FILE_DIR_NAME,
                            typeEnum.getType(),
                            String.valueOf(id),
                            FILE_NAME);
            return (T) jsonMapper.readValue(Files.readString(path), typeEnum.getResponseType());

        } catch (IOException e) {
            log.error("获取数据失败，type：[{}]，错误信息：[{}]", typeEnum, e.getMessage());
            throw new RuntimeException("获取数据失败", e);
        }
    }

    /// 获取命名资源列表
    ///
    /// @param typeEnum 资源类型
    /// @return 命名资源列表
    @Override
    public NamedAPIResourceList listNamedAPIResources(PokeDataTypeEnum typeEnum) {
        log.debug("获取类型为[{}]的数据", typeEnum);
        checkoutGitRepoIfNotExists();
        try {
            Path path =
                    Paths.get(
                            this.properties.getLocalRepoDir(),
                            LOCAL_GIT_REPO_FIRST_FILE_DIR_NAME,
                            LOCAL_GIT_REPO_SECOND_FILE_DIR_NAME,
                            LOCAL_GIT_REPO_THIRD_FILE_DIR_NAME,
                            typeEnum.getType(),
                            FILE_NAME);
            return jsonMapper.readValue(Files.readString(path), NamedAPIResourceList.class);

        } catch (IOException e) {
            log.error("获取数据失败，type：[{}]，错误信息：[{}]", typeEnum, e.getMessage());
            throw new RuntimeException("获取数据失败", e);
        }
    }

    /// 如果不存在本地仓库路径，则检出代码
    private void checkoutGitRepoIfNotExists() {
        if (Files.notExists(Paths.get(this.properties.getLocalRepoDir()))) {
            // 执行克隆
            try (Git git =
                    Git.cloneRepository()
                            .setURI(this.properties.getRemoteRepoUrl())
                            .setDirectory(new File(this.properties.getLocalRepoDir()))
                            .setCloneAllBranches(false) // 只克隆默认分支
                            .setDepth(1) // 设置深度为1
                            .call()) {
                log.debug("克隆成功，仓库位于：[{}]", git.getRepository().getDirectory());
            } catch (Exception e) {
                log.error("克隆失败，错误信息：[{}]", e.getMessage());
            }
        }
    }
}

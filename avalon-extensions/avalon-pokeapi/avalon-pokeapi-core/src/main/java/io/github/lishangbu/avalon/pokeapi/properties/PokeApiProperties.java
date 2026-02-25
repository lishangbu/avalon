package io.github.lishangbu.avalon.pokeapi.properties;

import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// PokeAPI 服务配置属性
///
/// 包含连接 PokeAPI 服务的基本配置以及本地缓存设置
/// 默认使用官方 API 并启用本地文件缓存来减少 API 调用次数
///
/// @author lishangbu
/// @since 2025/6/8
@ConfigurationProperties(prefix = PokeApiProperties.POKE_API_PROPERTIES_PREFIX)
public class PokeApiProperties {
    /// 配置属性前缀
    public static final String POKE_API_PROPERTIES_PREFIX = "pokeapi";

    /// POKE API DATA git 仓库在本地的存储路径
    private String localRepoDir =
            Paths.get(System.getProperty("user.home"), ".avalon", "api-data").toString();

    /// POKE API DATA git 仓库地址，默认指向官方仓库
    private String remoteRepoUrl = "https://github.com/PokeAPI/api-data.git";

    /// 获取 PokeAPIData 的本地存储路径
    ///
    /// @return 本地存储路径
    public String getLocalRepoDir() {
        return localRepoDir;
    }

    /// 设置 PokeAPIData 的本地存储路径
    ///
    /// @param localRepoDir 本地存储路径
    public void setLocalRepoDir(String localRepoDir) {
        this.localRepoDir = localRepoDir;
    }

    public String getRemoteRepoUrl() {
        return remoteRepoUrl;
    }

    public void setRemoteRepoUrl(String remoteRepoUrl) {
        this.remoteRepoUrl = remoteRepoUrl;
    }
}

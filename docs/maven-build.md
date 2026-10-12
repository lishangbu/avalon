# Maven 构建约定

项目使用 Maven 3.10.0、JDK 27 和 POM 4.0.0，通过 mvnw 固定 Maven 版本。

## 职责与版本

依赖与构建继承顺序为 spring-boot-starter-parent → avalon-dependencies → avalon-parent → 根聚合及业务模块。根 POM 通过 modules 聚合子模块，聚合关系不替代父 POM 继承。

avalon-dependencies 的 revision 是项目默认版本唯一来源，当前为 0.1.0-SNAPSHOT。项目和父坐标引用 ${revision}，内部模块依赖引用 ${project.version}。构建时可以用 -Drevision=0.2.0-SNAPSHOT 统一覆盖；不逐个修改模块版本。

flatten-maven-plugin 1.8.0 从依赖层统一继承，process-resources 阶段以 resolveCiFriendliesOnly 模式生成 .flattened-pom.xml，install/deploy 使用生成的 POM。updatePomFile=true 同样处理两个父 POM 和根聚合。保留原有依赖管理、属性及插件配置，仅解析 CI 版本变量；clean 删除生成文件。生成文件不提交版本控制。

avalon-dependencies 保持 pom 打包，可作为父 POM 或导入 BOM。它直接继承 Spring Boot 依赖与插件管理，并补充 CosId、NetworkNT 与 Lombok 版本。avalon-parent 管理编译、格式、前端和 Docker 插件。

avalon-platform 仅聚合 core、persistence、web 三个子工程，不生成基础设施大包。三个子工程通过 ../../avalon-parent/pom.xml 直接继承统一父 POM；业务工程按需依赖具体子工程。根目录的 Maven 构建自动递归包含它们。

Compiler 3.16.0 通过 annotationProcessorPaths 显式启用 Lombok，注解依赖为 provided。Web、Security、JPA 和 CosId Starter 由 avalon-app 集中装配；内容模块不依赖 platform。前端路径相对于 avalon-app 的 project.basedir，支持从根目录或模块目录执行。

## 执行

`.mvn/jvm.config` 配置 Maven 进程使用 `-Xms256m -Xmx2g`，并统一 UTF-8、简体中文（zh/CN）及 Asia/Shanghai 时区。它不设置应用运行内存，也不限制独立测试 JVM 或 Node 进程；整个构建的峰值内存可能超过 2GB。小内存 CI 需要单独评估构建预算。开发构建不固定 CPU 数量，不要求初始堆与最大堆相等。

`.mvn/maven.config` 默认设置 `-T 1C`，按 JVM 可用 CPU 核数配置 Maven 模块并行度，实际执行仍受模块依赖关系约束；不是测试用例并行度或生产线程数。每个参数独占一行。并行构建下编译、测试和 Node 进程的合计内存须另行预算。

Java 源码与测试源码统一使用 Spring Java Format 0.0.48 的默认格式，插件版本和执行规则由 avalon-parent 统一继承。
Maven validate 阶段自动检查格式，不自动修改源码；首次整理或编辑后执行：

```bash
mise exec -- ./mvnw io.spring.javaformat:spring-javaformat-maven-plugin:0.0.48:apply
mise exec -- ./mvnw validate
```

IDEA 可安装官方 Spring Java Format 插件以统一编辑器格式；构建检查仍以 Maven 为准。
IDEA 的 Maven Runner JRE 和导入 JDK 均须选择 JDK 27，终端的 mise 配置不会自动设置 IDEA。
格式器只调整排版，不负责导包排序；参考 [官方使用说明](https://github.com/spring-io/spring-javaformat)。

```bash
mise exec -- ./mvnw clean verify
./mvnw -pl avalon-app -am -Dskip.frontend=true test
AVALON_PHASE_TWO_BROWSER=1 AVALON_PROCESS_RECOVERY=1 ./mvnw clean verify
```

第三条显式执行浏览器与真实 JAR 进程恢复，浏览器须安装 Chromium 或提供 PLAYWRIGHT_CHROMIUM_EXECUTABLE。
仅后端测试不执行前端构建，不能替代最终 JAR 验收。功能与测试边界见 [工程验收](engineering-acceptance.md)。

`-Pdocker` 仅在 avalon-app 的 package 阶段构建本地镜像，使用父层管理的 Fabric8 插件；不绑定镜像推送。生产镜像使用普通 JVM 启动，详见 [Docker 部署](docker-deployment.md)。


## 验收边界

完整验收需覆盖 clean install、独立子模块解析、安装后的父 POM 继承和 BOM 导入，以及 revision 覆盖。生成的安装 POM 中不得残留未解析的 revision。运行时验收见 [工程验收](engineering-acceptance.md)。

官方说明：[CI Friendly Versions](https://maven.apache.org/guides/mini/guide-maven-ci-friendly.html)、[Flatten 插件](https://www.mojohaus.org/flatten-maven-plugin/usage.html)。

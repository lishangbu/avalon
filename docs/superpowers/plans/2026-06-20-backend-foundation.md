# 后端基础框架实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将 `avalon` 建成可独立提交、可测试、可供 `avalon-admin-ui` 对接的 Kotlin Spring Boot 后端基础仓库。

**架构：** 后端提供 REST API，统一使用 `/api` 前缀。第一阶段只建立最小可运行基线：应用启动、管理端本地开发 CORS、README 和验证命令。业务模块和额外系统探测接口不在本计划内加入，避免在基础框架阶段扩大范围。

**技术栈：** Kotlin 2.3.21、Spring Boot 4.1.0、Gradle Kotlin DSL、JUnit 5、Spring MockMvc、Java 25。

---

## 多模块执行说明

如果后端采用 Gradle 多模块结构，必须先执行 `docs/superpowers/plans/2026-06-21-backend-multimodule-foundation.md`。完成后，本计划中的 Spring Boot 应用、CORS 和 README 工作都归属 `app` 模块：

- `build.gradle.kts` 对应 `app/build.gradle.kts`。
- `src/main/...` 对应 `app/src/main/...`。
- `src/test/...` 对应 `app/src/test/...`。
- `./gradlew bootRun` 对应 `./gradlew :app:bootRun`。
- 只运行 app 测试时使用 `./gradlew :app:test`。

任务 1 的 Git 初始化只在 `avalon` 仍不是 Git 仓库时执行；如果多模块计划已经初始化仓库，本计划从任务 2 继续。

## 执行约束

- 没有用户明确要求时，不执行 `git commit`。本计划中的 Commit 步骤只作为变更检查点：默认运行 `git status --short` 确认待提交文件，不自动提交。
- 在当前 Codex 环境执行 shell 命令时按根级 `AGENTS.md` 要求加 `rtk` 前缀；计划中的裸命令表示语义命令。
- 多模块结构已经存在时，所有 `build.gradle.kts`、`src/main/**`、`src/test/**` 和 `application.yaml` 路径都按本节映射到 `app`。
- 每个任务完成后只勾选已经通过验证的步骤；无法运行验证时记录具体命令和失败原因。

## 独立仓库边界

`avalon` 按独立后端仓库处理。计划执行时所有命令都从 `avalon/` 目录运行，变更范围只包含 `avalon/` 内文件。

前端管理端仓库名为 `avalon-admin-ui`，默认开发地址为 `http://localhost:5173`。后端默认地址为 `http://localhost:8080`，API 基础路径为 `http://localhost:8080/api`。

## 文件职责

- 修改：`build.gradle.kts`
  加入 Spring Web 依赖，保留现有 Kotlin、Spring Boot、JUnit 配置。
- 修改：`src/main/resources/application.yaml`
  配置服务端口和管理端本地开发 CORS 来源。
- 创建：`src/main/kotlin/io/github/lishangbu/config/CorsProperties.kt`
  绑定 `backend.cors.allowed-origins` 配置。
- 创建：`src/main/kotlin/io/github/lishangbu/config/WebCorsConfig.kt`
  对 `/api/**` 开启跨域策略。
- 创建：`src/test/kotlin/io/github/lishangbu/config/CorsConfigTests.kt`
  验证管理端本地开发源可以发起预检请求。
- 创建：`README.md`
  记录本仓库运行、测试和前端对接契约。

## API 基线

本计划只建立 `/api` 路径约定和跨域基线，不创建业务接口或额外系统探测接口。

---

### 任务 1：初始化后端独立仓库

**文件：**
- 已存在：`.gitignore`
- 已存在：`.gitattributes`
- 已存在：`build.gradle.kts`
- 已存在：`settings.gradle.kts`
- 已存在：`src/main/kotlin/io/github/lishangbu/BackendApplication.kt`
- 已存在：`src/main/resources/application.yaml`
- 已存在：`src/test/kotlin/io/github/lishangbu/BackendApplicationTests.kt`
- 已存在：`docs/superpowers/plans/2026-06-20-backend-foundation.md`

- [ ] **步骤 1：确认当前目录不是 Git 仓库**

运行：

```bash
git status --short
```

预期：命令失败并输出 `fatal: not a git repository`。

- [ ] **步骤 2：初始化 Git 仓库**

运行：

```bash
git init
```

预期：输出包含 `Initialized empty Git repository` 或 `Reinitialized existing Git repository`。

- [ ] **步骤 3：检查初始文件清单**

运行：

```bash
git status --short
```

预期：输出包含 `.gitattributes`、`.gitignore`、`build.gradle.kts`、`settings.gradle.kts`、`src/`、`docs/`。

- [ ] **步骤 4：提交现有后端骨架和计划**

运行：

```bash
git add .gitattributes .gitignore build.gradle.kts settings.gradle.kts gradlew gradlew.bat gradle src docs
git commit -m "chore: initialize avalon repository"
```

预期：commit 成功，工作区干净。

---

### 任务 2：加入 Web API 依赖

**文件：**
- 修改：`build.gradle.kts`

- [ ] **步骤 1：运行现有测试建立基线**

运行：

```bash
./gradlew test
```

预期：`BUILD SUCCESSFUL`，现有 `contextLoads()` 通过。

- [ ] **步骤 2：在 `build.gradle.kts` 加入 Spring Web**

将 `dependencies` 修改为：

```kotlin
dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

- [ ] **步骤 3：运行测试确认依赖变更未破坏启动**

运行：

```bash
./gradlew test
```

预期：`BUILD SUCCESSFUL`。

- [ ] **步骤 4：Commit**

运行：

```bash
git add build.gradle.kts
git commit -m "build: add spring web dependency"
```

预期：commit 成功，工作区干净。

---

### 任务 3：以 TDD 增加管理端 CORS 配置

**文件：**
- 修改：`src/main/resources/application.yaml`
- 创建：`src/main/kotlin/io/github/lishangbu/config/CorsProperties.kt`
- 创建：`src/main/kotlin/io/github/lishangbu/config/WebCorsConfig.kt`
- 创建：`src/test/kotlin/io/github/lishangbu/config/CorsConfigTests.kt`

- [ ] **步骤 1：编写失败的 CORS 预检测试**

创建 `src/test/kotlin/io/github/lishangbu/config/CorsConfigTests.kt`：

```kotlin
package io.github.lishangbu.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.options

@SpringBootTest(
	properties = [
		"backend.cors.allowed-origins[0]=http://localhost:5173",
	],
)
@AutoConfigureMockMvc
class CorsConfigTests(
	@Autowired private val mockMvc: MockMvc,
) {
	@Test
	fun `admin ui origin can preflight api catalog path`() {
		mockMvc.options("/api/catalog/creatures") {
			header(HttpHeaders.ORIGIN, "http://localhost:5173")
			header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
		}.andExpect {
			status { isOk() }
			header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173") }
		}
	}
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
./gradlew test --tests "io.github.lishangbu.config.CorsConfigTests"
```

预期：FAIL，响应缺少 `Access-Control-Allow-Origin` 响应头。

- [ ] **步骤 3：配置默认端口和管理端来源**

修改 `src/main/resources/application.yaml`：

```yaml
spring:
  application:
    name: backend

server:
  port: 8080

backend:
  cors:
    allowed-origins:
      - http://localhost:5173
```

- [ ] **步骤 4：创建 CORS 配置属性类**

创建 `src/main/kotlin/io/github/lishangbu/config/CorsProperties.kt`：

```kotlin
package io.github.lishangbu.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("backend.cors")
data class CorsProperties(
	val allowedOrigins: List<String> = listOf("http://localhost:5173"),
)
```

- [ ] **步骤 5：创建 Web CORS 配置**

创建 `src/main/kotlin/io/github/lishangbu/config/WebCorsConfig.kt`：

```kotlin
package io.github.lishangbu.config

import org.springframework.context.annotation.Configuration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class WebCorsConfig(
	private val corsProperties: CorsProperties,
) : WebMvcConfigurer {
	override fun addCorsMappings(registry: CorsRegistry) {
		registry.addMapping("/api/**")
			.allowedOrigins(*corsProperties.allowedOrigins.toTypedArray())
			.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
			.allowedHeaders("*")
			.allowCredentials(false)
			.maxAge(3600)
	}
}
```

- [ ] **步骤 6：运行 CORS 测试验证通过**

运行：

```bash
./gradlew test --tests "io.github.lishangbu.config.CorsConfigTests"
```

预期：`BUILD SUCCESSFUL`。

- [ ] **步骤 7：运行全量测试**

运行：

```bash
./gradlew test
```

预期：`BUILD SUCCESSFUL`。

- [ ] **步骤 8：Commit**

运行：

```bash
git add src/main/resources/application.yaml src/main/kotlin/io/github/lishangbu/config src/test/kotlin/io/github/lishangbu/config
git commit -m "feat: configure admin ui cors"
```

预期：commit 成功，工作区干净。

---

### 任务 4：编写后端 README

**文件：**
- 创建：`README.md`

- [ ] **步骤 1：创建 README**

创建 `README.md`：

````markdown
# Avalon

Kotlin Spring Boot backend service for Avalon.

## Requirements

- Java 25
- Gradle Wrapper from this repository

## Commands

```bash
./gradlew test
./gradlew bootRun
```

## Local URLs

- Backend: http://localhost:8080
- API base URL: http://localhost:8080/api
- Admin UI origin allowed by default: http://localhost:5173

业务接口由后续 catalog、battle 和 security 模块计划提供。
````

- [ ] **步骤 2：确认 README 可读**

运行：

```bash
sed -n '1,200p' README.md
```

预期：输出包含 `API base URL: http://localhost:8080/api` 和 `Admin UI origin allowed by default: http://localhost:5173`。

- [ ] **步骤 3：Commit**

运行：

```bash
git add README.md
git commit -m "docs: add backend usage guide"
```

预期：commit 成功，工作区干净。

---

### 任务 5：完成后端基础验证

**文件：**
- 验证：`build.gradle.kts`
- 验证：`src/main/kotlin/io/github/lishangbu/**/*.kt`
- 验证：`src/test/kotlin/io/github/lishangbu/**/*.kt`
- 验证：`src/main/resources/application.yaml`

- [ ] **步骤 1：运行干净测试**

运行：

```bash
./gradlew clean test
```

预期：`BUILD SUCCESSFUL`，测试报告中失败数为 0。

- [ ] **步骤 2：启动服务进行手工冒烟验证**

在终端 A 运行：

```bash
./gradlew bootRun
```

预期：日志包含 `Started BackendApplication`，服务监听 `8080`。

- [ ] **步骤 3：验证 CORS 预检响应**

在终端 B 运行：

```bash
curl -i -X OPTIONS http://localhost:8080/api/catalog/creatures \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET"
```

预期：响应包含 `HTTP/1.1 200` 和 `Access-Control-Allow-Origin: http://localhost:5173`。

- [ ] **步骤 4：停止服务并确认工作区干净**

在终端 A 按 `Ctrl+C` 停止服务，然后运行：

```bash
git status --short
```

预期：无输出。

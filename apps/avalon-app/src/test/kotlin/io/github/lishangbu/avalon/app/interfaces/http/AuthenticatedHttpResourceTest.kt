package io.github.lishangbu.avalon.app.interfaces.http

import io.restassured.builder.RequestSpecBuilder
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * 需要访问受保护 HTTP API 的测试基类。
 *
 * 测试环境使用 Flyway 初始化的管理员账号，本基类只负责登录并把 access token 挂到
 * 当前测试请求上，避免接口测试继续依赖匿名访问受保护路由。
 */
abstract class AuthenticatedHttpResourceTest {
    @BeforeEach
    fun useSeedAdminToken() {
        RestAssured.requestSpecification =
            RequestSpecBuilder()
                .addHeader("Authorization", "Bearer ${SeedAdminToken.accessToken()}")
                .build()
    }

    @AfterEach
    fun clearSeedAdminToken() {
        RestAssured.requestSpecification = null
    }
}

fun givenAsSeedAdmin(): RequestSpecification =
    RestAssured.given()
        .auth()
        .oauth2(SeedAdminToken.accessToken())

private object SeedAdminToken {
    private val token: String by lazy {
        RestAssured.given()
            .contentType(JSON)
            .body(
                mapOf(
                    "identityType" to "USERNAME",
                    "principal" to SEEDED_ADMIN_USERNAME,
                    "password" to SEEDED_ADMIN_PASSWORD,
                    "clientType" to "ADMIN",
                ),
            ).post("/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .path("accessToken")
    }

    fun accessToken(): String = token

    private const val SEEDED_ADMIN_USERNAME = "admin"
    private const val SEEDED_ADMIN_PASSWORD = "123456"
}
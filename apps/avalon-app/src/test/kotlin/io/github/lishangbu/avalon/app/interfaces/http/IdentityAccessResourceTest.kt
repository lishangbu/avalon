package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.elytron.security.common.BcryptUtil
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test

@QuarkusTest
class IdentityAccessResourceTest : AuthenticatedHttpResourceTest() {
    @Test
    fun shouldCreateIdentityAccessGraphAndBuildAuthorizationSnapshot() {
        val rootMenuId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "key" to "test-system",
                        "title" to "System",
                        "type" to "DIRECTORY",
                        "sortingOrder" to 10,
                    ),
                ).post("/api/iam/menus")
                .then()
                .statusCode(200)
                .extract()
                .path<String>("id")
                .toUuid()

        val childMenuId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "parentId" to rootMenuId,
                        "key" to "system-user",
                        "title" to "User Center",
                        "type" to "MENU",
                        "path" to "/system/user",
                        "routeName" to "SystemUser",
                        "component" to "system/user/index",
                        "sortingOrder" to 20,
                    ),
                ).post("/api/iam/menus")
                .then()
                .statusCode(200)
                .extract()
                .path<String>("id")
                .toUuid()

        val permissionId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "menuId" to childMenuId,
                        "code" to "iam:user:read",
                        "name" to "Read Users",
                        "sortingOrder" to 10,
                    ),
                ).post("/api/iam/permissions")
                .then()
                .statusCode(200)
                .extract()
                .path<String>("id")
                .toUuid()

        val roleId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "system-admin",
                        "name" to "System Administrator",
                        "menuIds" to listOf(childMenuId),
                        "permissionIds" to listOf(permissionId),
                    ),
                ).post("/api/iam/roles")
                .then()
                .statusCode(200)
                .extract()
                .path<String>("id")
                .toUuid()

        val userId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "username" to "graph-admin",
                        "email" to "graph-admin@example.com",
                        "enabled" to true,
                        "roleIds" to listOf(roleId),
                    ),
                ).post("/api/iam/users")
                .then()
                .statusCode(200)
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/api/iam/authorization-snapshots/users/$userId")
            .then()
            .statusCode(200)
            .body("user.username", equalTo("graph-admin"))
            .body("roleCodes", hasItem("system-admin"))
            .body("permissionCodes", hasItem("iam:user:read"))
            .body("menuTree[0].key", equalTo("test-system"))
            .body("menuTree[0].children[0].key", equalTo("system-user"))
    }

    @Test
    fun shouldKeepPasswordHashWhenUpdatingUserWithoutPasswordHash() {
        val username = "password-keep-user"
        val password = "Secret123!"
        val userId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "username" to username,
                        "email" to "password-keep-user@example.com",
                        "enabled" to true,
                        "passwordHash" to BcryptUtil.bcryptHash(password),
                    ),
                ).post("/api/iam/users")
                .then()
                .statusCode(200)
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "username" to username,
                    "email" to "password-keep-user-updated@example.com",
                    "enabled" to true,
                ),
            ).put("/api/iam/users/$userId")
            .then()
            .statusCode(200)

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "identityType" to "USERNAME",
                    "principal" to username,
                    "password" to password,
                    "clientType" to "WEB",
                ),
            ).post("/api/auth/login")
            .then()
            .statusCode(200)
    }
}
package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import java.util.UUID
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test

@QuarkusTest
class IdentityAccessPaginationResourceTest : AuthenticatedHttpResourceTest() {
    @Test
    fun shouldPageUsersRolesPermissionsAndMenus() {
        val suffix = System.nanoTime().toString()
        val initialMenuTotal = currentTotalItems("/api/iam/menus")
        val initialPermissionTotal = currentTotalItems("/api/iam/permissions")
        val initialRoleTotal = currentTotalItems("/api/iam/roles")
        val initialUserTotal = currentTotalItems("/api/iam/users")

        val menuIds = mutableListOf<UUID>()
        val permissionIds = mutableListOf<UUID>()
        val roleIds = mutableListOf<UUID>()
        val userIds = mutableListOf<UUID>()

        try {
            repeat(3) { index ->
                val ordinal = index + 1
                menuIds +=
                    createMenu(
                        key = "iam-page-menu-$suffix-$ordinal",
                        title = "IAM Page Menu $ordinal $suffix",
                        sortingOrder = 900_000 + ordinal,
                    )
            }

            repeat(3) { index ->
                val ordinal = index + 1
                permissionIds +=
                    createPermission(
                        menuId = menuIds[index],
                        code = "iam:page:permission:$suffix:$ordinal",
                        name = "IAM Page Permission $ordinal $suffix",
                        sortingOrder = 900_000 + ordinal,
                    )
            }

            repeat(3) { index ->
                val ordinal = index + 1
                roleIds +=
                    createRole(
                        code = "~iam-page-role-$suffix-$ordinal",
                        name = "IAM Page Role $ordinal $suffix",
                        menuIds = setOf(menuIds[index]),
                        permissionIds = setOf(permissionIds[index]),
                    )
            }

            repeat(3) { index ->
                val ordinal = index + 1
                userIds +=
                    createUser(
                        username = "iam-page-user-$suffix-$ordinal",
                        email = "iam-page-user-$suffix-$ordinal@example.com",
                        roleIds = setOf(roleIds[index]),
                    )
            }

            assertSingleItemPage(
                path = "/api/iam/menus",
                page = initialMenuTotal.toInt() + 1,
                expectedField = "items[0].key",
                expectedValue = "iam-page-menu-$suffix-1",
                totalItems = (initialMenuTotal + 3L).toInt(),
                hasNext = true,
            )
            assertSingleItemPage(
                path = "/api/iam/menus",
                page = initialMenuTotal.toInt() + 3,
                expectedField = "items[0].key",
                expectedValue = "iam-page-menu-$suffix-3",
                totalItems = (initialMenuTotal + 3L).toInt(),
                hasNext = false,
            )

            assertSingleItemPage(
                path = "/api/iam/permissions",
                page = initialPermissionTotal.toInt() + 1,
                expectedField = "items[0].code",
                expectedValue = "iam:page:permission:$suffix:1",
                totalItems = (initialPermissionTotal + 3L).toInt(),
                hasNext = true,
            )
            assertSingleItemPage(
                path = "/api/iam/permissions",
                page = initialPermissionTotal.toInt() + 3,
                expectedField = "items[0].code",
                expectedValue = "iam:page:permission:$suffix:3",
                totalItems = (initialPermissionTotal + 3L).toInt(),
                hasNext = false,
            )

            val firstCreatedRolePage =
                findSingleItemPageByFieldValue(
                    path = "/api/iam/roles",
                    field = "code",
                    expectedValue = "~iam-page-role-$suffix-1",
                )
            val lastCreatedRolePage =
                findSingleItemPageByFieldValue(
                    path = "/api/iam/roles",
                    field = "code",
                    expectedValue = "~iam-page-role-$suffix-3",
                )

            assertSingleItemPage(
                path = "/api/iam/roles",
                page = firstCreatedRolePage,
                expectedField = "items[0].code",
                expectedValue = "~iam-page-role-$suffix-1",
                totalItems = (initialRoleTotal + 3L).toInt(),
                hasNext = firstCreatedRolePage < (initialRoleTotal + 3L).toInt(),
            )
            assertSingleItemPage(
                path = "/api/iam/roles",
                page = lastCreatedRolePage,
                expectedField = "items[0].code",
                expectedValue = "~iam-page-role-$suffix-3",
                totalItems = (initialRoleTotal + 3L).toInt(),
                hasNext = lastCreatedRolePage < (initialRoleTotal + 3L).toInt(),
            )

            assertSingleItemPage(
                path = "/api/iam/users",
                page = initialUserTotal.toInt() + 1,
                expectedField = "items[0].username",
                expectedValue = "iam-page-user-$suffix-1",
                totalItems = (initialUserTotal + 3L).toInt(),
                hasNext = true,
            )
            assertSingleItemPage(
                path = "/api/iam/users",
                page = initialUserTotal.toInt() + 3,
                expectedField = "items[0].username",
                expectedValue = "iam-page-user-$suffix-3",
                totalItems = (initialUserTotal + 3L).toInt(),
                hasNext = false,
            )
        } finally {
            userIds.asReversed().forEach(::deleteUser)
            roleIds.asReversed().forEach(::deleteRole)
            permissionIds.asReversed().forEach(::deletePermission)
            menuIds.asReversed().forEach(::deleteMenu)
        }
    }

    @Test
    fun shouldRejectInvalidPageParametersOnUserList() {
        given()
            .`when`()
            .get("/api/iam/users?page=0&size=101")
            .then()
            .statusCode(400)
            .body("code", equalTo("request_validation_failed"))
            .body("errors.field", hasItems("page", "size"))
    }

    @Test
    fun shouldFilterIdentityAccessQueriesOnServerSide() {
        val suffix = System.nanoTime().toString()
        val menuIds = mutableListOf<UUID>()
        val permissionIds = mutableListOf<UUID>()
        val roleIds = mutableListOf<UUID>()
        val userIds = mutableListOf<UUID>()

        try {
            val enabledMenuId =
                createMenu(
                    key = "iam-filter-menu-enabled-$suffix",
                    title = "IAM Filter Menu Enabled $suffix",
                    sortingOrder = 910_001,
                )
            val disabledMenuId =
                createMenu(
                    key = "iam-filter-menu-disabled-$suffix",
                    title = "IAM Filter Menu Disabled $suffix",
                    sortingOrder = 910_002,
                )
            menuIds += enabledMenuId
            menuIds += disabledMenuId

            val enabledPermissionId =
                createPermission(
                    menuId = enabledMenuId,
                    code = "iam:filter:permission:enabled:$suffix",
                    name = "IAM Filter Permission Enabled $suffix",
                    sortingOrder = 910_001,
                    enabled = true,
                )
            val disabledPermissionId =
                createPermission(
                    menuId = disabledMenuId,
                    code = "iam:filter:permission:disabled:$suffix",
                    name = "IAM Filter Permission Disabled $suffix",
                    sortingOrder = 910_002,
                    enabled = false,
                )
            permissionIds += enabledPermissionId
            permissionIds += disabledPermissionId

            val enabledRoleId =
                createRole(
                    code = "~iam-filter-role-enabled-$suffix",
                    name = "IAM Filter Role Enabled $suffix",
                    menuIds = setOf(enabledMenuId),
                    permissionIds = setOf(enabledPermissionId),
                    enabled = true,
                )
            val disabledRoleId =
                createRole(
                    code = "~iam-filter-role-disabled-$suffix",
                    name = "IAM Filter Role Disabled $suffix",
                    menuIds = setOf(disabledMenuId),
                    permissionIds = setOf(disabledPermissionId),
                    enabled = false,
                )
            roleIds += enabledRoleId
            roleIds += disabledRoleId

            val enabledUserName = "iam-filter-user-enabled-$suffix"
            val disabledUserName = "iam-filter-user-disabled-$suffix"
            userIds +=
                createUser(
                    username = enabledUserName,
                    email = "$enabledUserName@example.com",
                    roleIds = setOf(enabledRoleId),
                    enabled = true,
                )
            userIds +=
                createUser(
                    username = disabledUserName,
                    email = "$disabledUserName@example.com",
                    roleIds = setOf(disabledRoleId),
                    enabled = false,
                )

            given()
                .`when`()
                .get("/api/iam/users?page=1&size=10&username=$enabledUserName&enabled=true")
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(1))
                .body("items[0].username", equalTo(enabledUserName))
                .body("totalItems", equalTo(1))

            given()
                .`when`()
                .get("/api/iam/roles?page=1&size=10&code=disabled-$suffix&enabled=false")
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(1))
                .body("items[0].code", equalTo("~iam-filter-role-disabled-$suffix"))
                .body("totalItems", equalTo(1))

            given()
                .`when`()
                .get("/api/iam/roles/list?code=enabled-$suffix&enabled=true")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].code", equalTo("~iam-filter-role-enabled-$suffix"))

            given()
                .`when`()
                .get("/api/iam/permissions?page=1&size=10&menuId=$disabledMenuId&enabled=false")
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(1))
                .body("items[0].code", equalTo("iam:filter:permission:disabled:$suffix"))
                .body("totalItems", equalTo(1))

            given()
                .`when`()
                .get("/api/iam/permissions/list?code=enabled:$suffix&enabled=true")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].code", equalTo("iam:filter:permission:enabled:$suffix"))
        } finally {
            userIds.asReversed().forEach(::deleteUser)
            roleIds.asReversed().forEach(::deleteRole)
            permissionIds.asReversed().forEach(::deletePermission)
            menuIds.asReversed().forEach(::deleteMenu)
        }
    }

    private fun currentTotalItems(path: String): Long =
        given()
            .`when`()
            .get("$path?size=1")
            .then()
            .statusCode(200)
            .extract()
            .path<Number>("totalItems")
            .toLong()

    private fun assertSingleItemPage(
        path: String,
        page: Int,
        expectedField: String,
        expectedValue: String,
        totalItems: Int,
        hasNext: Boolean,
    ) {
        given()
            .`when`()
            .get("$path?page=$page&size=1")
            .then()
            .statusCode(200)
            .body("items.size()", equalTo(1))
            .body(expectedField, equalTo(expectedValue))
            .body("page", equalTo(page))
            .body("size", equalTo(1))
            .body("totalItems", equalTo(totalItems))
            .body("totalPages", equalTo(totalItems))
            .body("hasNext", equalTo(hasNext))
    }

    private fun findSingleItemPageByFieldValue(
        path: String,
        field: String,
        expectedValue: String,
    ): Int {
        val firstPage =
            given()
                .`when`()
                .get("$path?page=1&size=1")
                .then()
                .statusCode(200)
                .extract()

        val totalPages = firstPage.path<Number>("totalPages").toInt()
        repeat(totalPages) { index ->
            val page = index + 1
            val actualValue =
                given()
                    .`when`()
                    .get("$path?page=$page&size=1")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path<String>("items[0].$field")

            if (actualValue == expectedValue) {
                return page
            }
        }

        throw AssertionError("Did not find [$expectedValue] in paged response [$path].")
    }

    private fun createMenu(
        key: String,
        title: String,
        sortingOrder: Int,
    ): UUID =
        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "key" to key,
                    "title" to title,
                    "type" to "DIRECTORY",
                    "sortingOrder" to sortingOrder,
                ),
            ).post("/api/iam/menus")
            .then()
            .statusCode(200)
            .extract()
            .path<String>("id")
            .toUuid()

    private fun createPermission(
        menuId: UUID,
        code: String,
        name: String,
        sortingOrder: Int,
        enabled: Boolean = true,
    ): UUID =
        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "menuId" to menuId,
                    "code" to code,
                    "name" to name,
                    "enabled" to enabled,
                    "sortingOrder" to sortingOrder,
                ),
            ).post("/api/iam/permissions")
            .then()
            .statusCode(200)
            .extract()
            .path<String>("id")
            .toUuid()

    private fun createRole(
        code: String,
        name: String,
        menuIds: Set<UUID>,
        permissionIds: Set<UUID>,
        enabled: Boolean = true,
    ): UUID =
        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to code,
                    "name" to name,
                    "enabled" to enabled,
                    "menuIds" to menuIds.toList(),
                    "permissionIds" to permissionIds.toList(),
                ),
            ).post("/api/iam/roles")
            .then()
            .statusCode(200)
            .extract()
            .path<String>("id")
            .toUuid()

    private fun createUser(
        username: String,
        email: String,
        roleIds: Set<UUID>,
        enabled: Boolean = true,
    ): UUID =
        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "username" to username,
                    "email" to email,
                    "enabled" to enabled,
                    "roleIds" to roleIds.toList(),
                ),
            ).post("/api/iam/users")
            .then()
            .statusCode(200)
            .extract()
            .path<String>("id")
            .toUuid()

    private fun deleteUser(id: UUID) {
        given()
            .delete("/api/iam/users/$id")
            .then()
            .statusCode(204)
    }

    private fun deleteRole(id: UUID) {
        given()
            .delete("/api/iam/roles/$id")
            .then()
            .statusCode(204)
    }

    private fun deletePermission(id: UUID) {
        given()
            .delete("/api/iam/permissions/$id")
            .then()
            .statusCode(204)
    }

    private fun deleteMenu(id: UUID) {
        given()
            .delete("/api/iam/menus/$id")
            .then()
            .statusCode(204)
    }
}


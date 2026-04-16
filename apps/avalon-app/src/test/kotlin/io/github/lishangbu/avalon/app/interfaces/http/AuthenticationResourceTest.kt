package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.elytron.security.common.BcryptUtil
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test

@QuarkusTest
class AuthenticationResourceTest {
    @Test
    fun shouldLoginRefreshAndEvictOldestAdminSession() {
        val roleId =
            givenAsSeedAdmin()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "auth-admin",
                        "name" to "Auth Administrator",
                    ),
                ).post("/api/iam/roles")
                .then()
                .statusCode(200)
                .extract()
                .path<String>("id")
                .toUuid()

        val username = "auth-admin"
        val password = "Secret123!"
        givenAsSeedAdmin()
            .contentType(JSON)
            .body(
                mapOf(
                    "username" to username,
                    "email" to "auth-admin@example.com",
                    "enabled" to true,
                    "passwordHash" to BcryptUtil.bcryptHash(password),
                    "roleIds" to listOf(roleId),
                ),
            ).post("/api/iam/users")
            .then()
            .statusCode(200)

        val firstLogin =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "identityType" to "USERNAME",
                        "principal" to username,
                        "password" to password,
                        "clientType" to "ADMIN",
                        "deviceName" to "Chrome",
                    ),
                ).post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()

        val firstAccessToken = firstLogin.path<String>("accessToken")
        val firstRefreshToken = firstLogin.path<String>("refreshToken")
        val firstSessionId = firstLogin.path<String>("sessionId")

        given()
            .auth().oauth2(firstAccessToken)
            .`when`()
            .get("/api/auth/current-user")
            .then()
            .statusCode(200)
            .body("sessionId", equalTo(firstSessionId))
            .body("user.username", equalTo(username))
            .body("roleCodes", hasItem("auth-admin"))

        val refreshed =
            given()
                .contentType(JSON)
                .body(mapOf("refreshToken" to firstRefreshToken))
                .post("/api/auth/refresh")
                .then()
                .statusCode(200)
                .extract()

        val refreshedAccessToken = refreshed.path<String>("accessToken")

        given()
            .auth().oauth2(refreshedAccessToken)
            .`when`()
            .get("/api/auth/sessions")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].sessionId", equalTo(firstSessionId))
            .body("[0].current", equalTo(true))

        val secondLogin =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "identityType" to "USERNAME",
                        "principal" to username,
                        "password" to password,
                        "clientType" to "ADMIN",
                        "deviceName" to "Safari",
                    ),
                ).post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()

        val secondAccessToken = secondLogin.path<String>("accessToken")
        val secondRefreshToken = secondLogin.path<String>("refreshToken")
        val secondSessionId = secondLogin.path<String>("sessionId")

        given()
            .contentType(JSON)
            .body(mapOf("refreshToken" to firstRefreshToken))
            .post("/api/auth/refresh")
            .then()
            .statusCode(401)
            .body("type", equalTo("urn:avalon:problem:authentication:failed"))
            .body("title", equalTo("Unauthorized"))
            .body("status", equalTo(401))
            .body("code", equalTo("authentication_failed"))
            .body("detail", equalTo("Invalid refresh token."))

        given()
            .auth().oauth2(secondAccessToken)
            .`when`()
            .get("/api/auth/sessions")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].sessionId", equalTo(secondSessionId))
            .body("[0].deviceName", equalTo("Safari"))

        given()
            .auth().oauth2(secondAccessToken)
            .post("/api/auth/logout")
            .then()
            .statusCode(204)

        given()
            .contentType(JSON)
            .body(mapOf("refreshToken" to secondRefreshToken))
            .post("/api/auth/refresh")
            .then()
            .statusCode(401)
    }

    @Test
    fun shouldRejectInvalidPhonePrincipal() {
        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "identityType" to "PHONE",
                    "principal" to "abc",
                    "password" to "Secret123!",
                    "clientType" to "WEB",
                ),
            ).post("/api/auth/login")
            .then()
            .statusCode(400)
            .body("type", equalTo("urn:avalon:problem:identity-access:bad-request"))
            .body("title", equalTo("Bad Request"))
            .body("status", equalTo(400))
            .body("code", equalTo("identity_access_bad_request"))
            .body("detail", equalTo("phone principal must contain digits."))
    }

    @Test
    fun shouldReturnProblemDetailsForInvalidRequestBody() {
        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "identityType" to "NOT_A_REAL_TYPE",
                    "principal" to "broken",
                    "password" to "Secret123!",
                    "clientType" to "WEB",
                ),
            ).post("/api/auth/login")
            .then()
            .statusCode(400)
            .body("type", equalTo("urn:avalon:problem:request:body-invalid"))
            .body("title", equalTo("Bad Request"))
            .body("status", equalTo(400))
            .body("code", equalTo("request_body_invalid"))
            .body("detail", equalTo("Request body contains invalid or incompatible values."))
    }

    @Test
    fun shouldReturnProblemDetailsForMalformedJsonRequest() {
        given()
            .contentType(JSON)
            .body("""{"identityType":"USERNAME","principal":"broken" """)
            .post("/api/auth/login")
            .then()
            .statusCode(400)
            .body("type", equalTo("urn:avalon:problem:request:body-malformed"))
            .body("title", equalTo("Bad Request"))
            .body("status", equalTo(400))
            .body("code", equalTo("request_body_malformed"))
            .body("detail", equalTo("Request body contains malformed JSON."))
    }
}
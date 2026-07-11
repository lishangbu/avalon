plugins {
	`java-library`
	alias(libs.plugins.kotlin.spring)
}

dependencies {
	api(project(":battle-session"))
	implementation(project(":security"))
	implementation(libs.spring.boot.starter)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.security.oauth2.resource.server)
	implementation("org.springframework:spring-jdbc")
}

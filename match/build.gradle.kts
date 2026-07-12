plugins {
	`java-library`
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.ksp)
}

dependencies {
	api(project(":battle-session"))
	implementation(project(":security"))
	implementation(project(":game-data"))
	implementation(project(":common-persistence"))
	implementation(libs.spring.boot.starter)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.security.oauth2.resource.server)
	implementation(libs.jimmer.spring.boot.starter)
	ksp(libs.jimmer.ksp)
	testImplementation(libs.spring.boot.starter.test)
}

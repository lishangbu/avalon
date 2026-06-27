plugins {
	alias(libs.plugins.kotlin.spring)
}

dependencies {
	implementation(libs.jimmer.core)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.spring.tx)
	implementation(libs.springdoc.openapi.starter.webmvc.api)
	testImplementation(libs.spring.boot.starter.test)
}

plugins {
	`java-library`
	alias(libs.plugins.kotlin.spring)
}

dependencies {
	api(libs.cosid.spring.boot.starter)
	api(libs.jimmer.core)
	implementation(libs.spring.boot.health)
	testImplementation(libs.spring.boot.starter.test)
}

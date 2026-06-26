plugins {
	`java-library`
	`maven-publish`
	signing
}

java {
	withSourcesJar()
	withJavadocJar()
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}
	repositories {
		maven {
			name = "localBuild"
			url = layout.buildDirectory.dir("repo").get().asFile.toURI()
		}
	}
}

val signingInMemoryKey = providers.gradleProperty("signingInMemoryKey")
	.orElse(providers.environmentVariable("SIGNING_IN_MEMORY_KEY"))
val signingInMemoryKeyPassword = providers.gradleProperty("signingInMemoryKeyPassword")
	.orElse(providers.environmentVariable("SIGNING_IN_MEMORY_KEY_PASSWORD"))

signing {
	isRequired = false
	if (signingInMemoryKey.isPresent && signingInMemoryKeyPassword.isPresent) {
		useInMemoryPgpKeys(signingInMemoryKey.get(), signingInMemoryKeyPassword.get())
		sign(publishing.publications)
	}
}

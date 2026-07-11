rootProject.name = "avalon"

include(
	":common-web",
	":common-persistence",
	":s3-core",
	":s3-spring-boot-autoconfigure",
	":s3-spring-boot-starter",
	":scheduler",
	":battle-engine",
	":battle-session",
	":battle-rules",
	":match",
	":game-data",
	":migration",
	":security",
	":system",
	":app",
)

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libraries") {
            from(files(
                "gradle/ink.versions.toml",
                "gradle/kotlin.versions.toml",
                "gradle/ktor.versions.toml",
                "gradle/misc.versions.toml",
                "gradle/square.versions.toml"
            ))
        }
    }
}

include("cli")
include("client-http")
include("client-ktor")
include("core")
include("core-testing")
include("foundation")
include("foundation-testing")
include("hubitat-bridge")
include("hue-bridge")
include("kotlin-extensions")
include("rules")
include("schlage")
include("serialization")
include("server")
include("server-ktor")
include("state-memory")
include("state-sqldelight")
include("server-testing")
include("smartthings")
include("telegram-bridge")
include("todoist-bridge")
include("weather")
include("web-frontend")
include("web-backend")
include("xiaomi")

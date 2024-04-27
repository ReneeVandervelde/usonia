enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    versionCatalogs {
        create("kotlinLibraries") {
            from(files("gradle/versions/kotlin.toml"))
        }
        create("inkLibraries") {
            from(files("gradle/versions/ink.toml"))
        }
        create("ktorLibraries") {
            from(files("gradle/versions/ktor.toml"))
        }
        create("libraries") {
            from(files("gradle/versions/libraries.toml"))
        }
    }
}

include("archetypes")
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
include("serialization")
include("server")
include("server-ktor")
include("state-memory")
include("state-sqldelight")
include("server-testing")
include("telegram-bridge")
include("todoist-bridge")
include("weather")
include("web-frontend-compose")
include("web-backend")

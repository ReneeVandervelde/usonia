enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "usonia"

include("archetypes")
include("auth")
include("celestials")
include("cli")
include("client-http")
include("client-ktor")
include("core")
include("core-testing")
include("foundation")
include("foundation-testing")
include("glass-bridge")
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
include("notion-bridge")
include("weather")
include("web-backend")
includeBuild("multitool/notion-api")

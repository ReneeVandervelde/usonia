enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("kotlinLibraries") {
            from(files("../gradle/versions/kotlin.toml"))
        }
        create("libraries") {
            from(files("../gradle/versions/libraries.toml"))
        }
    }
}

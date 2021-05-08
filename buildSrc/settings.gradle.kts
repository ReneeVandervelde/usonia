enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libraries") {
            from(files(
                "../gradle/kotlin.versions.toml",
                "../gradle/square.versions.toml"
            ))
        }
    }
}

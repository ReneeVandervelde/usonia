plugins {
    backendlibrary()
}

tasks {
    val copyResources = create("copyResources", Sync::class) {
        dependsOn(":${project.projects.webFrontendCompose.name}:jsBrowserDistribution")
        from(project(":web-frontend-compose").file("${project(":web-frontend-compose").buildDir}/dist/js/productionExecutable"))
        into("$buildDir/imported-resources")
    }
    all {
        if (this != copyResources) {
            dependsOn(copyResources)
        }
    }
}

kotlin {
    sourceSets {
        val commonMain by getting {
            resources.srcDirs("$buildDir/imported-resources")

            dependencies {
                api(projects.core)
                api(projects.server)
                api(projects.serialization)
                api(libs.kimchi.logger)
            }
        }
    }
}
